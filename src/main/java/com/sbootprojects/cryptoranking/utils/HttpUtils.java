package com.sbootprojects.cryptoranking.utils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Collections;

// NOT a component..cz no use of sprigs radar on it
public class HttpUtils {

    private static String apiHost = "coinranking1.p.rapidapi.com";
    private static String apiKey = "743bc88666msh6d3436814a78e37p1c2521jsne02604513f52";

    public static HttpEntity<String> getHttpEntity(){
        System.out.println("apiHost: " + apiHost);
        System.out.println("apiKey: " + apiKey);

        // to add headers to the entity
        HttpHeaders headers = new HttpHeaders();

        // to specify we will get JSON as response
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // add the headers
        headers.set("X-RapidAPI-Host", apiHost);
        headers.set("X-RapidAPI-Key", apiKey);

        //return the enity with headers .. (body, headers)
        return new HttpEntity<>(null, headers);
    }
}
