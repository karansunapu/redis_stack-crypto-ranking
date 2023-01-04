package com.sbootprojects.cryptoranking.service;

import com.google.gson.Gson;
import com.sbootprojects.cryptoranking.model.*;
import com.sbootprojects.cryptoranking.utils.HttpUtils;
import io.github.dengliming.redismodule.redisjson.RedisJSON;
import io.github.dengliming.redismodule.redisjson.args.GetArgs;
import io.github.dengliming.redismodule.redisjson.args.SetArgs;
import io.github.dengliming.redismodule.redisjson.utils.GsonUtils;
import io.github.dengliming.redismodule.redistimeseries.DuplicatePolicy;
import io.github.dengliming.redismodule.redistimeseries.RedisTimeSeries;
import io.github.dengliming.redismodule.redistimeseries.Sample;
import io.github.dengliming.redismodule.redistimeseries.TimeSeriesOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CoinsDataService {


    // public static final
    public static final String GET_COINS_API = "https://coinranking1.p.rapidapi.com/coins?referenceCurrencyUuid=yhjMzLPhuIDl&timePeriod=24h&tiers=1&orderBy=marketCap&orderDirection=desc&limit=50&offset=0";
    public static final String GET_COIN_HISTORY_API = "https://coinranking1.p.rapidapi.com/coin/";
    public static final String COIN_HISTORY_TIME_PERIOD_PARAM = "/history?timePeriod=";
    public static final List<String> timePeriods = Arrays.asList("24h", "7d", "30d", "3m", "1y", "3y", "5y");
    public static final String REDIS_KEY_COINS = "coins";

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    RedisJSON redisJSON;
    @Autowired
    RedisTimeSeries redisTimeSeries;

    // methods public void ---- not static ... use autowired objects to call methods

    //make this method to call on app startup so include it in ApplicationStartup class method
    public void fetchCoins() {
        log.info("Inside fetchCoins()");
        ResponseEntity<Coins> coinsResponseEntityEntity = restTemplate.exchange(GET_COINS_API,
                HttpMethod.GET,
                HttpUtils.getHttpEntity(),
                Coins.class);

        storeCoinsToRedisJSON(coinsResponseEntityEntity.getBody());
        log.info("All Coins Data Saved to Redis");
    }

    public void fetchCoinHistoryData(){
        log.info("Inside fetchCoinHistoryData()");
        List<CoinInfo> allCoins = getAllCoinsFromRedisJSON();

        allCoins.forEach(coinInfo -> {
            timePeriods.forEach(s -> {
                try {
                    fetchCoinHistoryForTimePeriod(coinInfo, s);
                    Thread.sleep(200); // To Avoid Rate Limit of rapid API of 5 Request/Sec
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private void fetchCoinHistoryForTimePeriod(CoinInfo coinInfo, String timePeriod) {
        log.info("Fetching Coin History of {} for Time Period {}", coinInfo.getName(), timePeriod);
        String url = GET_COIN_HISTORY_API + coinInfo.getUuid() + COIN_HISTORY_TIME_PERIOD_PARAM + timePeriod;
        ResponseEntity<CoinPriceHistory> coinPriceHistoryResponseEntity = restTemplate.exchange(url,
                HttpMethod.GET,
                HttpUtils.getHttpEntity(),
                CoinPriceHistory.class);
        log.info("Data Fetched From API for Coin History of {} for Time Period {}", coinInfo.getName(), timePeriod);

        storeCoinHistoryToRedisTS(coinPriceHistoryResponseEntity.getBody(), coinInfo.getSymbol(), timePeriod);
    }

    private void storeCoinHistoryToRedisTS(CoinPriceHistory coinPriceHistory, String symbol, String timePeriod) {
        log.info("Storing Coin History of {} for Time Period {} into Redis TS", symbol, timePeriod);
        List<CoinPriceHistoryExchangeRate> coinExchangeRates =
                coinPriceHistory.getData().getHistory();

        // symbol:timePeriod
        // BTC:24h, ETH:24h, ETH:3y
        coinExchangeRates.stream()
                .filter(ch -> ch.getPrice() != null && ch.getTimestamp() != null)
                .forEach(ch -> redisTimeSeries.add(
                        new Sample(symbol + ":" + timePeriod, Sample.Value.of(Long.valueOf(ch.getTimestamp()), Double.valueOf(ch.getPrice()))),
                        new TimeSeriesOptions()
                                // can compress the data too
                                .unCompressed()
                                //in case of duplicates takes the latest one
                                .duplicatePolicy(DuplicatePolicy.LAST)
                                //can add labels also -> .labels(new Label(symbol, timePeriod)));
                ));
        log.info("Complete: Stored Coin History of {} for Time Period {} into Redis TS", symbol, timePeriod);
    }

    private List<CoinInfo> getAllCoinsFromRedisJSON() {
        CoinData coinData = redisJSON.get(REDIS_KEY_COINS,
                CoinData.class,
                new GetArgs().path(".data").indent("\t").newLine("\n").space(" "));
        log.info("allCoins: " + coinData);
        return coinData.getCoins();
    }

    private void storeCoinsToRedisJSON(Coins coins) {

        // create(path, String json) -> path is from where u want to take json ..we want
        // whole json so -> "."
        // GsonUtils(not Gson) -> to convert Coins Object to String Json
        redisJSON.set(REDIS_KEY_COINS,
                SetArgs.Builder.create(".", GsonUtils.toJson(coins)));
    }

    public List<CoinInfo> fetchAllCoinsFromRedisJSON() {
        return getAllCoinsFromRedisJSON();
    }

    public List<Sample.Value> fetchCoinHistoryPerTimePeriodFromRedisTS(String symbol, String timePeriod) {
        String key = symbol + ":" + timePeriod;
        //time_series.info returns a map of values
        // to use the last & first timestamp values for the TS.RANGE query
        Map<String, Object> tsInfo = redisTimeSeries.info(key);
        String firstTimeStamp = tsInfo.get("firstTimestamp").toString();
        String lastTimestamp = tsInfo.get("lastTimestamp").toString();

        List<Sample.Value> coinsTSData = redisTimeSeries.range(
                key, Long.valueOf(firstTimeStamp), Long.valueOf(lastTimestamp));

        return coinsTSData;
    }
}
