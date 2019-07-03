package com.ringly.ringly.config;

import com.ringly.ringly.config.model.GuidedMeditation;

import java.util.List;

import retrofit2.http.GET;
import rx.Observable;

public interface GuidedMeditationsService {

    @GET("guided-meditations")
    Observable<List<GuidedMeditation>> getGuidedMeditations();

}
