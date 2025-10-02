#include <jni.h>
#include <string>

//Development Bucket Region
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_developmentAWSBucketRegion(JNIEnv *env, jobject object) {
    std::string bucketRegion = "ap-south-1";
    return env->NewStringUTF(bucketRegion.c_str());
}

//Live Bucket Region
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_liveAWSBucketRegion(JNIEnv *env, jobject object) {
    std::string bucketRegion = "us-east-1";
    return env->NewStringUTF(bucketRegion.c_str());
}


//Development Secret Key
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_developmentAWSSecretKey(JNIEnv *env, jobject object) {
    std::string AWSSecretKey = "";
    return env->NewStringUTF(AWSSecretKey.c_str());
}

//Live Secret Key
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_liveAWSSecretKey(JNIEnv *env, jobject object) {
    std::string AWSSecretKey = "yc0bMKPDPt3K7tigJvRGGB86oHvHwVMiwCwvZSjH";
    return env->NewStringUTF(AWSSecretKey.c_str());
}


//Development Access Key
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_developmentAWSAccessKey(JNIEnv *env, jobject object) {
    std::string AWSAccessKey = "";
    return env->NewStringUTF(AWSAccessKey.c_str());
}

//Live Access Key
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_liveAWSAccessKey(JNIEnv *env, jobject object) {
    std::string AWSAccessKey = "AKIAW5XMEM3H5ZZM5WWS";
    return env->NewStringUTF(AWSAccessKey.c_str());
}


//Development AWS Base URl
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_developmentAWSBaseURL(JNIEnv *env, jobject object) {
    std::string AWSBaseUrl = "";
    return env->NewStringUTF(AWSBaseUrl.c_str());
}

//Live AWS Base URl
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_liveAWSBaseURL(JNIEnv *env, jobject object) {
    std::string AWSBaseUrl = "https://sameteam.s3.amazonaws.com/";
    return env->NewStringUTF(AWSBaseUrl.c_str());
}


//Development AWS Bucket Name
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_developmentAWSBucketName(JNIEnv *env, jobject object) {
    std::string AWSBucketName = """";
    return env->NewStringUTF(AWSBucketName.c_str());
}

//Live AWS AWS Bucket Name
extern "C" JNIEXPORT jstring
Java_com_example_sameteam_helper_Keys_liveAWSBucketName(JNIEnv *env, jobject object) {
    std::string AWSBucketName = "sameteam";
    return env->NewStringUTF(AWSBucketName.c_str());
}