package com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.downloader.OnCancelListener
import com.downloader.OnDownloadListener
//import com.downloader.PRDownloader
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivityShowImageBinding
import com.example.sameteam.helper.ImagePickerActivity
import com.example.sameteam.helper.Utils.getPermissionAsPerAndroidVersion
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.viewModel.ShowImageVM
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File


class ShowImageActivity : BaseActivity<ActivityShowImageBinding>() {
    var fileName: String? = null


    override fun layoutID() = com.example.sameteam.R.layout.activity_show_image

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    ).get(ShowImageVM::class.java)


    lateinit var binding: ActivityShowImageBinding
    lateinit var showImageVM: ShowImageVM
    override fun initActivity(mBinding: ViewDataBinding) {
       // PRDownloader.initialize(getApplicationContext());

        binding = mBinding as ActivityShowImageBinding
        showImageVM = getViewModel() as ShowImageVM

        binding.customToolbar.rightIcon.visibility = View.GONE
        binding.customToolbar.downloadIcon.visibility = View.VISIBLE


//        binding.customToolbar.title.text = getString(com.example.sameteam.R.string.image)
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }
        binding.customToolbar.title.text = getString(com.example.sameteam.R.string.app_name)

        val imageUrl = intent.getStringExtra("imageUrl")
        fileName = intent.getStringExtra("fileName")
        Log.e("7-7-2023", imageUrl.toString())

        binding.customToolbar.downloadIcon.setOnClickListener {


            //downloadFile(imageUrl ?: "")
            //TODO check Permission
            Dexter.withActivity(this)
                .withPermissions(getPermissionAsPerAndroidVersion())
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
                            binding.progressBar.visibility = View.VISIBLE
                            downloadURL(imageUrl ?: "")
                        }
                        else{
                            Toast.makeText(this@ShowImageActivity,"Please allow storage permission from settings",Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: List<PermissionRequest>,
                        token: PermissionToken
                    ) {
                        token.continuePermissionRequest()
                    }
                }).check()


        }
        fileName?.let {

            Log.e("23/01 fileName -=-=-=>", it)

            if (it.contains("pdf") && imageUrl != null) {
                //PDF

                binding.customToolbar.title.text = getString(com.example.sameteam.R.string.pdf)

                binding.ivProduct.visibility = View.GONE
                binding.webView1.visibility = View.VISIBLE

                Log.e("23/01 imageUrl -=-=-=>", imageUrl)
//                    binding.pdfViewPager.fromUri(Uri.parse(imageUrl))
//                    RetrivePDFfromUrl().execute(pdfurl);


                binding.webView1.settings.javaScriptEnabled = true;
//                    binding.webView1.getSettings().setPluginState(WebSettings.PluginState.ON);
                //---you need this to prevent the webview from
                // launching another browser when a url
                // redirection occurs---
                binding.webView1.settings.builtInZoomControls = true;
                binding.webView1.settings.setSupportZoom(true);
                binding.webView1.settings.useWideViewPort = true;
                binding.webView1.settings.loadWithOverviewMode = true
                binding.webView1.webViewClient = Callback(binding.progressBar);

//                    binding.progressBar.visibility = View.VISIBLE
                Log.e(
                    "24/01 imageUrl -=-=-=>",
                    "${"http://docs.google.com/gview?embedded=true&url=$imageUrl"}"
                )
                binding.webView1.loadUrl(
                    "http://docs.google.com/gview?embedded=true&url=" + imageUrl
                );


            }
            else if (imageUrl != null && (it.contains("doc") || it.contains("docx"))) {
                //doc or docx

                binding.ivProduct.visibility = View.GONE
                binding.webView1.visibility = View.VISIBLE

                binding.customToolbar.title.text = getString(com.example.sameteam.R.string.document)

                binding.webView1.settings.javaScriptEnabled = true;
//                    binding.webView1.getSettings().setPluginState(WebSettings.PluginState.ON);
                //---you need this to prevent the webview from
                // launching another browser when a url
                // redirection occurs---
                binding.webView1.settings.builtInZoomControls = true;
                binding.webView1.settings.setSupportZoom(true);
                binding.webView1.settings.useWideViewPort = true;
                binding.webView1.settings.loadWithOverviewMode = true
                binding.webView1.webViewClient = Callback(binding.progressBar);

                /* binding.webView1.loadUrl(
                     "http://docs.google.com/gview?embedded=true&url=" + imageUrl);*/


//                binding.progressBar.visibility = View.VISIBLE

                Log.e(
                    "24/01 imageUrl -=-=-=>",
                    "${"http://docs.google.com/gview?embedded=true&url=$imageUrl"}"
                )

                binding.webView1.loadUrl(
                    "http://docs.google.com/gview?embedded=true&url="
                            + imageUrl
                );

            }
            else if (imageUrl != null && (it.contains("xls") || it.contains("xlsx"))) {
                //doc or docx

                binding.ivProduct.visibility = View.GONE
                binding.webView1.visibility = View.VISIBLE

                binding.customToolbar.title.text = getString(com.example.sameteam.R.string.document)

                binding.webView1.settings.javaScriptEnabled = true;
                binding.webView1.clearCache(true)
   //                 binding.webView1.getSettings().setPluginState(WebSettings.PluginState.ON);
                //---you need this to prevent the webview from
                // launching another browser when a url
                // redirection occurs---
                binding.webView1.settings.builtInZoomControls = true;
                binding.webView1.settings.setSupportZoom(true);
                binding.webView1.settings.useWideViewPort = true;
                binding.webView1.settings.loadWithOverviewMode = true
                binding.webView1.webViewClient = Callback(binding.progressBar);

                /* binding.webView1.loadUrl(
                     "http://docs.google.com/gview?embedded=true&url=" + imageUrl);*/


//                binding.progressBar.visibility = View.VISIBLE

                Log.e(
                    "24/01 imageUrl -=-=-=>",
                    "${"http://docs.google.com/gview?embedded=true&url=$imageUrl"}"
                )

                binding.webView1.loadUrl(
                    "http://docs.google.com/gview?embedded=true&url="
                            + imageUrl
                );

            }
            else if (imageUrl != null && it.contains("csv")) {
                //doc or docx

                binding.ivProduct.visibility = View.GONE
                binding.webView1.visibility = View.VISIBLE

                binding.customToolbar.title.text = getString(com.example.sameteam.R.string.document)

                binding.webView1.settings.javaScriptEnabled = true;
//                    binding.webView1.getSettings().setPluginState(WebSettings.PluginState.ON);
                //---you need this to prevent the webview from
                // launching another browser when a url
                // redirection occurs---
                binding.webView1.settings.builtInZoomControls = true;
                binding.webView1.settings.setSupportZoom(true);
                binding.webView1.settings.useWideViewPort = true;
                binding.webView1.settings.loadWithOverviewMode = true
                binding.webView1.webViewClient = Callback(binding.progressBar);

                /* binding.webView1.loadUrl(
                     "http://docs.google.com/gview?embedded=true&url=" + imageUrl);*/


//                binding.progressBar.visibility = View.VISIBLE

                Log.e(
                    "24/01 imageUrl -=-=-=>",
                    "${"http://docs.google.com/gview?embedded=true&url=$imageUrl"}"
                )

                binding.webView1.loadUrl(
                    "http://docs.google.com/gview?embedded=true&url="
                            + imageUrl
                );

            } else if (imageUrl != null && it.contains("txt")) {
                //Txt

                binding.ivProduct.visibility = View.GONE
                binding.webView1.visibility = View.VISIBLE

                binding.customToolbar.title.text = getString(com.example.sameteam.R.string.document)

                binding.webView1.settings.javaScriptEnabled = true;
//                    binding.webView1.getSettings().setPluginState(WebSettings.PluginState.ON);
                //---you need this to prevent the webview from
                // launching another browser when a url
                // redirection occurs---
                binding.webView1.settings.builtInZoomControls = true;
                binding.webView1.settings.setSupportZoom(true);
                binding.webView1.settings.useWideViewPort = true;
                binding.webView1.settings.loadWithOverviewMode = true
                binding.webView1.webViewClient = Callback(binding.progressBar);

                /* binding.webView1.loadUrl(
                     "http://docs.google.com/gview?embedded=true&url=" + imageUrl);*/


//                binding.progressBar.visibility = View.VISIBLE

                Log.e(
                    "24/01 imageUrl -=-=-=>",
                    "${"http://docs.google.com/gview?embedded=true&url=$imageUrl"}"
                )

                binding.webView1.loadUrl(
                    "http://docs.google.com/gview?embedded=true&url="
                            + imageUrl
                );

            } else if (imageUrl != null && (it.contains("jpeg")
                        || it.contains("jpg")
                        || it.contains("png")
                        || it.contains("gif"))
            ) {
                binding.customToolbar.title.text = getString(com.example.sameteam.R.string.image)

                binding.ivProduct.visibility = View.VISIBLE
                binding.webView1.visibility = View.GONE
                try {
                    Glide.with(this)
                        .load(imageUrl)
                        .error(com.example.sameteam.R.drawable.image_placeholder)
                        .placeholder(com.example.sameteam.R.drawable.image_placeholder)
                        .into(binding.ivProduct)

                } catch (e: IllegalArgumentException) {
                    Log.d("AttachmentPreview", e.message!!)

                }

            } else {

            }

        }

        /*  Glide.with(this)
              .load(imageUrl)
              .error(R.drawable.image_placeholder)
              .placeholder(R.drawable.image_placeholder)
              .into(binding.ivProduct)
  */
    }

    private fun downloadURL(imageUrl: String?) {
//TODO Permission

        getDownloadDir(this@ShowImageActivity)?.path?.let { Log.e("10/07 -=-=-=-=>", it) }
        Log.e(
            "10/07  File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);-=-=-=-=>",
            fileName ?: ""
        )
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        Log.e("10/07 Download File dir =-=-=-=-=>", dir.path.toString() ?: "")

      /*  PRDownloader.download(imageUrl, dir.path, fileName)
            .build()
            .setOnStartOrResumeListener {

                Log.e("10/07 setOnStartOrResumeListener -=-=>", "setOnStartOrResumeListener")

            }
            .setOnPauseListener {
                Log.e("10/07 setOnPauseListener -=-=>", "setOnPauseListener")

            }
            .setOnCancelListener(object : OnCancelListener {
                override fun onCancel() {
                    binding.progressBar.visibility = View.GONE

                    Log.e("10/07 onCancel -=-=>", "onCancel")
                    Toast.makeText(this@ShowImageActivity, "Download cancelled", Toast.LENGTH_SHORT)
                        .show()

                }
            })
            .setOnProgressListener { }
            .start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    binding.progressBar.visibility = View.GONE
                    Log.e("10/07 onDownloadComplete -=-=>", "onDownloadComplete")
                    Toast.makeText(this@ShowImageActivity, "Download complete", Toast.LENGTH_SHORT)
                        .show()

                }

                override fun onError(error: com.downloader.Error?) {
                    binding.progressBar.visibility = View.GONE
                    error?.responseCode?.toString()?.let {
                        Log.e("10/07 onError -=-=>", Gson().toJson(error))
                        Toast.makeText(
                            this@ShowImageActivity,
                            "Something went wrong!",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }

            })*/
    }

    fun getDownloadDir(context: Context): File? {
        return if (isExternalStorageWritable()) context
            .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) else context.filesDir
    }

    fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }


}

private class Callback(var progressBar: ProgressBar) : WebViewClient() {


    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        progressBar.visibility = View.VISIBLE
    }

    override fun shouldOverrideUrlLoading(
        view: WebView, url: String
    ): Boolean {
        return false
    }

    override fun onPageFinished(view: WebView, url: String) {
        // TODO Auto-generated method stub
        super.onPageFinished(view, url)
        progressBar.visibility = View.GONE
    }

}
/*

class AppWebViewClients(bindingCustom: ActivityShowImageBinding) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }


    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        // TODO Auto-generated method stub
        view.loadUrl(url)
        return true
    }

    override fun onPageFinished(view: WebView, url: String) {
        // TODO Auto-generated method stub
        super.onPageFinished(view, url)
    }
}*/
