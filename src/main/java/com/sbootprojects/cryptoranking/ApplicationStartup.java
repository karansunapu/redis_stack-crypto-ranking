package com.sbootprojects.cryptoranking;


import com.sbootprojects.cryptoranking.service.CoinsDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

// to execute at startup of project implement
// ApplicationListener<ApplicationReadyEvent>
@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private CoinsDataService coinsDataService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
//        coinsDataService.fetchCoins();
//        coinsDataService.fetchCoinHistoryData();

        // can make a scheduler and run it everyday to fetch the latest data
    }
}
