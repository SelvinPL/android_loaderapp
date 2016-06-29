package pl.selvin.loaderapp;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class MainActivity extends Activity {

    private static DexClassLoader loader;
    private static ArrayList<String> loadedUris = new ArrayList<>();
    TextView text1;
    TextView text2;

    private final static String[][] setupData = new String[][]{
            new String[]{"http://selvin.pl/loaderappimpl1.jar", "pl.selvin.loaderapp.impl.Bar1"},
            new String[]{"http://selvin.pl/loaderappimpl2.jar", "pl.selvin.loaderapp.impl.Bar2"},
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text1 = (TextView) findViewById(R.id.text1);
        text2 = (TextView) findViewById(R.id.text2);
        new BackgroundLoader(text1).execute(setupData[0][0], setupData[0][1]);
        new BackgroundLoader(text2).execute(setupData[1][0], setupData[1][1]);
    }

    @Override
    public ClassLoader getClassLoader() {
        if (loader != null)
            return loader;
        return super.getClassLoader();
    }

    static class BackgroundLoader extends AsyncTask<String, Void, String> {
        private final TextView resultView;
        private final Context context;

        public BackgroundLoader(TextView resultView) {
            this.resultView = resultView;
            context = resultView.getContext();
        }

        @Override
        protected String doInBackground(String... urlAndClass) {
            if (urlAndClass != null && urlAndClass.length == 2) {
                final String url = urlAndClass[0];
                final String clazz = urlAndClass[1];
                try {

                    if (!loadedUris.contains(url)) {

                        final OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder().url(url).build();
                        Response response = client.newCall(request).execute();
                        if (!response.isSuccessful())
                            throw new IOException("Unexpected code " + response);

                        final File downloadedFile = File.createTempFile("temp", ".jar", context.getCacheDir());

                        BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
                        sink.writeAll(response.body().source());
                        sink.close();

                        final File tmpDir = context.getDir("dex", 0);
                        final String libPath = downloadedFile.getPath();
                        loader = new DexClassLoader(libPath, tmpDir.getAbsolutePath(), null, context.getClassLoader());
                        loadedUris.add(url);

                    }
                    Class<?> classToLoad = context.getClassLoader().loadClass(clazz);
                    return ((IFoo) classToLoad.newInstance()).doJob() + "(" + clazz + ")";

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onPostExecute(String result) {
            resultView.setText("result: " + result);
        }
    }
}
