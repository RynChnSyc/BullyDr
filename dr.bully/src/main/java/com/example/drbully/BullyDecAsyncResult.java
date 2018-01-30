package com.example.drbully;

import android.content.Context;

/**
 * Created by Chanoo on 1/16/2018.
 */

/**
 * Callback made by the asynchronous method of analyzing for emotions from a file.
 * @see BullyDec#AnalyzeForEmotionsAsync(Context, String, BullyDecAsyncResult)
 */

public interface BullyDecAsyncResult {

        /**
         * If there were no exceptions thrown by the native code during the analysis for emotions
         * @param emotionProbabilities The metrics of the emotions that were processed
         */
        void onSuccess(EmotionProbabilities emotionProbabilities);


        /**
         * If there was some problem that occurred while processing
         * @param e Exception describing the reason of the problem
         * @see BullyDecException#getErrorCode()
         */
        void onError(BullyDecException e);
    }


