package com.example.sameteam.amazonS3;

import android.content.Context;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.example.sameteam.BuildConfig;
import com.example.sameteam.helper.Keys;

public class S3Util {
    private static AmazonS3Client sS3Client;
    private static TransferUtility sTransferUtility;

    // set socket timeout as per needed default time out is 30 seconds
    private static final int SOCKET_TIMEOUT = 30 * 1000;
    // set connection timeout as per needed default time out is 30 seconds
    private static final int CONNECTION_TIMEOUT = 30 * 1000;
    // set maximum retry on error or cancel uploads
    private static final int MAX_RETRY = 3;


    public static AmazonS3Client getS3Client() {

        if (sS3Client == null) {

            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.withSocketTimeout(SOCKET_TIMEOUT);
            clientConfiguration.setConnectionTimeout(CONNECTION_TIMEOUT);
            clientConfiguration.setMaxErrorRetry(MAX_RETRY);
//            sS3Client = new AmazonS3Client(getCredProvider(context.getApplicationContext()));

            String accessKey = "";
            String secretKey = "";
            String bucketRegion = "";
            if(BuildConfig.FLAVOR == "client"){
                secretKey = Keys.INSTANCE.liveAWSSecretKey();
                accessKey = Keys.INSTANCE.liveAWSAccessKey();
                bucketRegion = Keys.INSTANCE.liveAWSBucketRegion();
            }
            else{
                secretKey = Keys.INSTANCE.developmentAWSSecretKey();
                accessKey = Keys.INSTANCE.developmentAWSAccessKey();
                bucketRegion = Keys.INSTANCE.developmentAWSBucketRegion();
            }

            sS3Client = new AmazonS3Client(new BasicAWSCredentials(
                    accessKey
                    , secretKey), Region.getRegion(Regions.fromName(bucketRegion)), clientConfiguration);

        }
        return sS3Client;
    }

    public static TransferUtility getTransferUtility(Context context) {
        if (sTransferUtility == null) {
            sTransferUtility = TransferUtility.builder().s3Client(getS3Client()).context(context).build();
        }

        return sTransferUtility;
    }
}
