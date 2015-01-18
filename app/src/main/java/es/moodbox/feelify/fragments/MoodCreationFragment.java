package es.moodbox.feelify.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.net.URL;

import es.moodbox.feelify.R;
import es.moodbox.feelify.activities.BasicActivity;
import es.moodbox.feelify.giphy.model.MoodModel;
import es.moodbox.feelify.giphy.model.SimpleModel;
import es.moodbox.feelify.giphy.services.GiphyServiceInterface;
import es.moodbox.feelify.services.FileDownloader;
import es.moodbox.feelify.utils.AppConstants;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MoodCreationFragment extends Fragment {

    private final static String TAG = MoodCreationFragment.class.getSimpleName();

    private WebView mWebView;
    private EditText mTextEdit;
    private MoodModel mMoodModel;
    private SimpleModel mSimpleModel;
    private View mLoading;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_mood_creation, container, false);
        mWebView = (WebView) v.findViewById(R.id.webView);

        mLoading = v.findViewById(R.id.loading_view);
        mLoading.setVisibility(View.GONE);

        ImageButton btShare = (ImageButton) v.findViewById(R.id.btShare);
        mTextEdit = (EditText) v.findViewById(R.id.editText);

        mMoodModel = (MoodModel) getActivity().getIntent().getSerializableExtra("model");

        btShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (mSimpleModel == null) {
                        somethingWentWrong();
                    }else {
                        new DownloadFilesTask().execute(new URL(mSimpleModel.mGiphyData.mUrl));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ups...." + e.getMessage());
                    somethingWentWrong();
                }
            }
        });
        return v;
    }

    private void somethingWentWrong() {
        Toast toast = Toast.makeText(getActivity(),"You just broke the app :_(",Toast.LENGTH_LONG);
        toast.show();
    }

    public void init() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://api.giphy.com/v1/stickers/")
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        showLoading();

        GiphyServiceInterface service = restAdapter.create(GiphyServiceInterface.class);
        String searchParameter = mMoodModel.searchTags.replace(",","+");
        service.random(searchParameter, new Callback<SimpleModel>() {

            @Override
            public void success(SimpleModel o, Response response) {
                mSimpleModel = o;
                mWebView.loadUrl(mSimpleModel.mGiphyData.mUrl);
                hideLoading();

            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("MainActivity", error.getMessage());
                hideLoading();
                somethingWentWrong();

            }
        });
    }

    public void onResume() {
        super.onResume();
        init();
    }
    public class DownloadFilesTask extends AsyncTask<URL, Integer, File> {

        protected File doInBackground(URL... urls) {
            ((BasicActivity) getActivity()).showActionBarSpinner();
            int count = urls.length;
            long totalSize = 0;
            File tmp = null;
            for (int i = 0; i < count; i++) {
                tmp = FileDownloader.download(getActivity(), AppConstants.IMG_NAME, ".gif", urls[i]);
                if (tmp != null) {
                    totalSize += tmp.length();
                    Log.d(TAG, "Downloaded total: " + totalSize + " url: " + urls[i].toString());
                }

                publishProgress((int) ((i / (float) count) * 100));
                // Escape early if cancel() is called
                if (isCancelled()) break;
            }
            return tmp;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(File result) {
            Uri path = Uri.fromFile(result);
            Log.d(TAG, "Download done, sharing file: " + path);
            try {
                sendFileWithUri(result);
            } catch (Exception e) {
                Log.e(TAG, "ups: " + e.getMessage());
                e.printStackTrace();
                ((BasicActivity) getActivity()).hideActionBarSpinner();
            }
            ((BasicActivity) getActivity()).hideActionBarSpinner();
        }
    }

    private void showLoading(){
        mLoading.setVisibility(View.VISIBLE);
    }

    private void hideLoading(){
        mLoading.setVisibility(View.GONE);
    }
    private void sendFileWithUri(File image) throws Exception {
        String message = String.valueOf(mTextEdit.getText());
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("image/*");
        //Uri path = Uri.fromFile(new File("android.resource://com.android.mypackage/drawable/arrow.png"));
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(image));
        startActivity(sendIntent);
    }
}
