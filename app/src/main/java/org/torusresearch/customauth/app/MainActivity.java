package org.torusresearch.customauth.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.bitcoinj.core.Base58;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.utils.TweetNaclFast;
import org.torusresearch.customauth.CustomAuth;
import org.torusresearch.customauth.types.AggregateLoginParams;
import org.torusresearch.customauth.types.AggregateVerifierType;
import org.torusresearch.customauth.types.Auth0ClientOptions;
import org.torusresearch.customauth.types.CustomAuthArgs;
import org.torusresearch.customauth.types.LoginType;
import org.torusresearch.customauth.types.LoginWindowResponse;
import org.torusresearch.customauth.types.NoAllowedBrowserFoundException;
import org.torusresearch.customauth.types.SubVerifierDetails;
import org.torusresearch.customauth.types.TorusAggregateLoginResponse;
import org.torusresearch.customauth.types.TorusKey;
import org.torusresearch.customauth.types.TorusLoginResponse;
import org.torusresearch.customauth.types.TorusVerifierResponse;
import org.torusresearch.customauth.types.TorusVerifierUnionResponse;
import org.torusresearch.customauth.types.UserCancelledException;
import org.torusresearch.customauth.utils.Helpers;
import org.torusresearch.customauth.utils.Triplet;
import org.torusresearch.fetchnodedetails.types.NodeDetails;
import org.torusresearch.fetchnodedetails.types.TorusNetwork;
import org.torusresearch.torusutils.types.TorusPublicKey;
import org.torusresearch.torusutils.types.VerifierArgs;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private final HashMap<String, LoginVerifier> verifierMap = new HashMap<String, LoginVerifier>() {
        {
            put("google", new LoginVerifier("Google", LoginType.GOOGLE, "221898609709-obfn3p63741l5333093430j3qeiinaa8.apps.googleusercontent.com", "google-lrc"));
            put("facebook", new LoginVerifier("Facebook", LoginType.FACEBOOK, "617201755556395", "facebook-lrc"));
            put("twitch", new LoginVerifier("Twitch", LoginType.TWITCH, "f5and8beke76mzutmics0zu4gw10dj", "twitch-lrc"));
            put("discord", new LoginVerifier("Discord", LoginType.DISCORD, "682533837464666198", "discord-lrc"));
            String domain = "torus-test.auth0.com";
            put("email_password", new LoginVerifier("Email Password", LoginType.EMAIL_PASSWORD, "sqKRBVSdwa4WLkaq419U7Bamlh5vK1H7", "torus-auth0-email-password", domain));
            put("apple", new LoginVerifier("Apple", LoginType.APPLE, "m1Q0gvDfOyZsJCZ3cucSQEe9XMvl9d9L", "torus-auth0-apple-lrc", domain));
            put("github", new LoginVerifier("Github", LoginType.GITHUB, "PC2a4tfNRvXbT48t89J5am0oFM21Nxff", "torus-auth0-github-lrc", domain));
            put("linkedin", new LoginVerifier("LinkedIn", LoginType.LINKEDIN, "59YxSgx79Vl3Wi7tQUBqQTRTxWroTuoc", "torus-auth0-linkedin-lrc", domain));
            put("twitter", new LoginVerifier("Twitter", LoginType.TWITTER, "A7H8kkcmyFRlusJQ9dZiqBLraG2yWIsO", "torus-auth0-twitter-lrc", domain));
            put("line", new LoginVerifier("Line", LoginType.APPLE, "WN8bOmXKNRH1Gs8k475glfBP5gDZr9H1", "torus-auth0-line-lrc", domain));
            put("hosted_email_passwordless", new LoginVerifier("Hosted Email Passwordless", LoginType.JWT, "P7PJuBCXIHP41lcyty0NEb7Lgf7Zme8Q", "torus-auth0-passwordless", domain, "name", false));
            put("hosted_sms_passwordless", new LoginVerifier("Hosted SMS Passwordless", LoginType.JWT, "nSYBFalV2b1MSg5b2raWqHl63tfH3KQa", "torus-auth0-sms-passwordless", domain, "name", false));
//            put("torus_passwordless", new LoginVerifier("Torus Passwordless", LoginType.JWT, "KG7zk89X3QgttSyX9NJ4fGEyFNhOcJTw", "tkey-auth0-email-passwordless-cyan", "auth.openlogin.com", "name", false));
        }
    };

    private final String[] allowedBrowsers = new String[]{
            "com.android.chrome", // Chrome stable
            "com.google.android.apps.chrome", // Chrome system
            "com.android.chrome.beta", // Chrome beta
    };

    private CustomAuth torusSdk;
    private LoginVerifier selectedLoginVerifier;
    private BigInteger privKey = null;
    private TextView textView;
    private ProgressBar pbLoader;
    private String authPublick = "0x2eE31d92Ca3C994B7A476f07dAD3cffc70EB7FB3";
    private BigInteger authPrivate = new BigInteger("85472410604589892354211069310546408403280615874058814307178516967716168789622");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.output);
        pbLoader = (ProgressBar) findViewById(R.id.pbLoader);

        // Option 1. Deep links if your OAuth provider supports it
        // DirectSdkArgs args = new DirectSdkArgs("torusapp://org.torusresearch.customauthandroid/redirect", TorusNetwork.TESTNET);

        // Option 2. Host redirect.html at your domain and proxy redirect to your app
        CustomAuthArgs args = new CustomAuthArgs("https://scripts.toruswallet.io/redirect.html", TorusNetwork.TESTNET, "torusapp://org.torusresearch.customauthandroid/redirect");
        // args.setEnableOneKey(true);

        // Initialize CustomAuth
        this.torusSdk = new CustomAuth(args, this);
       // this.torusSdk.nodeDetailManager.getNodeDetails()
        Spinner spinner = findViewById(R.id.verifierList);
        List<LoginVerifier> loginVerifierList = new ArrayList<>(verifierMap.values());
        ArrayAdapter<LoginVerifier> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, loginVerifierList);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }


    public void launch(View view) {
        singleLoginTest();
        // aggregateLoginTest();
    }

    public TweetNaclFast.Signature.KeyPair getEd25199Key(String privateKey) {
        byte[] decodedBytes = TweetNaclFast.hexDecode(privateKey);
        TweetNaclFast.Signature.KeyPair ed25519KeyPair = TweetNaclFast.Signature.keyPair_fromSeed(decodedBytes);
        return ed25519KeyPair;
    }

    public void createSolanaAccount(View view) {
        //TextView textView = findViewById(R.id.output);

        if (this.privKey == null) {
            textView.setText("Please login first to generate solana ed25519 key pair");
            return;
            //this.privKey = authPrivate;
        }
        pbLoader.setVisibility(View.VISIBLE);
        TweetNaclFast.Signature.KeyPair ed25519KeyPair = this.getEd25199Key(this.privKey.toString(16));
        Account SolanaAccount = new Account(ed25519KeyPair.getSecretKey());
        String pubKey = SolanaAccount.getPublicKey().toBase58();
        String secretKey = Base58.encode(SolanaAccount.getSecretKey());
        String accountInfo = String.format("Solana account secret key is %s and public Key %s", secretKey, pubKey);
        textView.setText(accountInfo);
        pbLoader.setVisibility(View.GONE);
    }

    public void getTorusKey(View view) throws ExecutionException, InterruptedException {
        pbLoader.setVisibility(View.VISIBLE);
        String verifier = "google-lrc";
        String verifierId = "hello@tor.us";
        HashMap<String, Object> verifierParamsHashMap = new HashMap<>();
        verifierParamsHashMap.put("verifier_id", verifierId);
        String idToken = "";
        NodeDetails nodeDetails = torusSdk.nodeDetailManager.getNodeDetails(verifier, verifierId).get();
        TorusPublicKey publicKey = torusSdk.torusUtils.getPublicAddress(nodeDetails.getTorusNodeEndpoints(), nodeDetails.getTorusNodePub(), new VerifierArgs(verifier, verifierId)).get();
        Log.d("public address", publicKey.getAddress());
        textView.setText(textView.getText()+"\n\n"+"Torus public address: "+publicKey.getAddress()/*+"\n\nnodeDetails: "+nodeDetails*/);
        pbLoader.setVisibility(View.GONE);
         //torusSdk.getTorusKey(verifier, verifierId, verifierParamsHashMap, idToken);
    }

    private void renderError(Throwable error) {
        Log.e("result:error", "error", error);
        Throwable reason = Helpers.unwrapCompletionException(error);
        if (reason instanceof UserCancelledException || reason instanceof NoAllowedBrowserFoundException)
            textView.setText(error.getMessage());
        else
            textView.setText("Something went wrong: " + error.getMessage());
    }

    @SuppressLint("SetTextI18n")
    public void singleLoginTest() {
        Log.d("result:selecteditem", this.selectedLoginVerifier.toString());
        Auth0ClientOptions.Auth0ClientOptionsBuilder builder = null;
        pbLoader.setVisibility(View.VISIBLE);

        if (this.selectedLoginVerifier.getDomain() != null) {
            builder = new Auth0ClientOptions.Auth0ClientOptionsBuilder(this.selectedLoginVerifier.getDomain());
            builder.setVerifierIdField(this.selectedLoginVerifier.getVerifierIdField());
            builder.setVerifierIdCaseSensitive(this.selectedLoginVerifier.isVerfierIdCaseSensitive());
        }
        CompletableFuture<TorusLoginResponse> torusLoginResponseCf;

        if (builder == null) {
            torusLoginResponseCf = this.torusSdk.triggerLogin(new SubVerifierDetails(this.selectedLoginVerifier.getTypeOfLogin(),
                    this.selectedLoginVerifier.getVerifier(),
                    this.selectedLoginVerifier.getClientId())
                    .setPreferCustomTabs(true)
                    .setAllowedBrowsers(allowedBrowsers));
        } else {
            torusLoginResponseCf = this.torusSdk.triggerLogin(new SubVerifierDetails(this.selectedLoginVerifier.getTypeOfLogin(),
                    this.selectedLoginVerifier.getVerifier(),
                    this.selectedLoginVerifier.getClientId(), builder.build())
                    .setPreferCustomTabs(true)
                    .setAllowedBrowsers(allowedBrowsers));
        }

        torusLoginResponseCf.whenComplete((torusLoginResponse, error) -> {
            if (error != null) {
                pbLoader.setVisibility(View.GONE);
                renderError(error);
            } else {

                String publicAddress = torusLoginResponse.getPublicAddress();
                this.privKey = torusLoginResponse.getPrivateKey();

                Log.d(MainActivity.class.getSimpleName(), publicAddress);
                textView.setText(publicAddress);
                pbLoader.setVisibility(View.GONE);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void aggregateLoginTest() {
        CompletableFuture<TorusAggregateLoginResponse> torusLoginResponseCf = this.torusSdk.triggerAggregateLogin(new AggregateLoginParams(AggregateVerifierType.SINGLE_VERIFIER_ID,
                "chai-google-aggregate-test", new SubVerifierDetails[]{
                new SubVerifierDetails(LoginType.GOOGLE, "google-chai", "884454361223-nnlp6vtt0me9jdsm2ptg4d1dh8i0tu74.apps.googleusercontent.com")
        }));

        torusLoginResponseCf.whenComplete((torusAggregateLoginResponse, error) -> {
            if (error != null) {
                renderError(error);
            } else {
                String json = torusAggregateLoginResponse.getPublicAddress();
                Log.d(MainActivity.class.getSimpleName(), json);
                ((TextView) findViewById(R.id.output)).setText(json);
            }
        });
    }

    public void myTry(View view){
        String domain = "torus-test.auth0.com";
        LoginVerifier loginVerifier = new LoginVerifier("Hosted Email Passwordless", LoginType.JWT, "P7PJuBCXIHP41lcyty0NEb7Lgf7Zme8Q",
                "torus-auth0-passwordless", domain, "name", false);
        this.selectedLoginVerifier = loginVerifier;

        String verifier = "google-lrc";
        String verifierId = "hello@tor.us";
        HashMap<String, Object> verifierParamsHashMap = new HashMap<>();
        verifierParamsHashMap.put("verifier_id", verifierId);
        String idToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImcyMWtLQ0tYTE04dmNQYlBNb1JWMSJ9.eyJuaWNrbmFtZSI6ImUwMTgzMzE4NDAyNyIsIm5hbWUiOiJlMDE4MzMxODQwMjdAZ21haWwuY29tIiwicGljdHVyZSI6Imh0dHBzOi8vcy5ncmF2YXRhci5jb20vYXZhdGFyL2Q0MjY2NzFiMjNjYWM1OTljY2I0Nzc1ZWZmZWU0YzM4P3M9NDgwJnI9cGcmZD1odHRwcyUzQSUyRiUyRmNkbi5hdXRoMC5jb20lMkZhdmF0YXJzJTJGZTAucG5nIiwidXBkYXRlZF9hdCI6IjIwMjMtMDUtMTBUMDg6NDQ6MjcuMzExWiIsImVtYWlsIjoiZTAxODMzMTg0MDI3QGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJpc3MiOiJodHRwczovL2Rldi1vYzFlZHhtYWJrcXNnajJkLmpwLmF1dGgwLmNvbS8iLCJhdWQiOiJDU2RZZDVoaTJKM2xYZ1pIVzh3bmZNdlVjRjhwOVBXcSIsImlhdCI6MTY4MzcwODI2NywiZXhwIjoxNjgzNzQ0MjY3LCJzdWIiOiJlbWFpbHw2NDViNTNjZWU4N2ZmNWRhYjQzMjY2ZmQifQ.ZZ77umQtgI9_oRgF-SgXemjRh9Y-I0Qti9szOg_rLNvWNKPY03JTQVyh2ZVsNN_Js0GyPsTPwDJ9Vv4RzqugMl_9LO5roGGKi4teB3kKxX9pPfGIY_ak3ddg4VXEoQeQ4s0Hv9RNTSsMdipx_x771hbOdNcTtj9GsQeEawBLcVzRbzfa_Ghc1QN8UGKHb7SF87jXL4hWPO6mo2s3ZONyDeuuAHK8DV-Mrpl89qjKfuFRt6dHZDDsSqgWfjIlep1qNBEwsx1pm08gS8ZR4sQzsF89Vz5HPmV1Zj4sIA7MVYYua82r1s7SB4r2ZLonkhAw8iWcjiNxRvYCuW2lfT3Dfg";
        //CompletableFuture<TorusKey> aa = torusSdk.getTorusKey(verifier, verifierId, verifierParamsHashMap, idToken).thenApply((torusKey) -> Pair.create(userInfoArray, torusKey));
        List<TorusVerifierResponse> userInfoArray = new ArrayList<>();

        /*private final String email;
        private final String name;
        private final String profileImage;
        private final String verifier;
        private final String verifierId;
        private final LoginType typeOfLogin;
        */
        TorusVerifierResponse userInfo = new TorusVerifierResponse(
                "e01833184027@gmail.com",
                "e01833184027@gmail.com",
                "https://s.gravatar.com/avatar/d426671b23cac599ccb4775effee4c38?s=480&r=pg&d=https%3A%2F%2Fcdn.auth0.com%2Favatars%2Fe0.png",
                "torus-auth0-passwordless",
                "e01833184027@gmail.com",
                LoginType.JWT
        );
        Log.d("cLog", "userInfo:" + userInfo.toString());

        /*
        private String accessToken;
        private String idToken;
        * */
        LoginWindowResponse response2 = new LoginWindowResponse();
        response2.setAccessToken("eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIiwiaXNzIjoiaHR0cHM6Ly9kZXYtb2MxZWR4bWFia3FzZ2oyZC5qcC5hdXRoMC5jb20vIn0..Qij8YbNUBCyVo4vf.A4hkTqBH-jV5GfZWFKVHFbZcdALY8MSEKKPErO-kzgeuNLZoe3kvqG71w6q5XSLn3v6a6SWQzEOhIiykqM756z0Gq65HFt-to3xJS3e9PvjHpW2WoBfAMlO0sHK3elaSRagygfC8WRhBs-s_ybxTxlTZfEJP_ANbFO1ynsRH1H2Rl2yPSdhRj9cCx1S5kKJFsBSuvs0rmohwQ5VLzlyQwtTQkzzdRwitadNnTpuz0wK1sZJgji9fBX3UfHXCnQh15r7YXlQz8pd9nZC5qUeoWf3lsbsMztJAmBfdvyMnjAFDL4VUjcd_tkjgxyRVe55diaWx5Mdr4KBjqoHrSDhVb1msA3wNn_Wul_eES8yC6D0JvsB2-v9QJBdT1vHjPX9L7w.qSAlwXSGeLNb1YuPycDjeg");
        response2.setIdToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImcyMWtLQ0tYTE04dmNQYlBNb1JWMSJ9.eyJuaWNrbmFtZSI6ImUwMTgzMzE4NDAyNyIsIm5hbWUiOiJlMDE4MzMxODQwMjdAZ21haWwuY29tIiwicGljdHVyZSI6Imh0dHBzOi8vcy5ncmF2YXRhci5jb20vYXZhdGFyL2Q0MjY2NzFiMjNjYWM1OTljY2I0Nzc1ZWZmZWU0YzM4P3M9NDgwJnI9cGcmZD1odHRwcyUzQSUyRiUyRmNkbi5hdXRoMC5jb20lMkZhdmF0YXJzJTJGZTAucG5nIiwidXBkYXRlZF9hdCI6IjIwMjMtMDUtMTBUMDg6NDQ6MjcuMzExWiIsImVtYWlsIjoiZTAxODMzMTg0MDI3QGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJpc3MiOiJodHRwczovL2Rldi1vYzFlZHhtYWJrcXNnajJkLmpwLmF1dGgwLmNvbS8iLCJhdWQiOiJDU2RZZDVoaTJKM2xYZ1pIVzh3bmZNdlVjRjhwOVBXcSIsImlhdCI6MTY4MzcwODI2NywiZXhwIjoxNjgzNzQ0MjY3LCJzdWIiOiJlbWFpbHw2NDViNTNjZWU4N2ZmNWRhYjQzMjY2ZmQifQ.ZZ77umQtgI9_oRgF-SgXemjRh9Y-I0Qti9szOg_rLNvWNKPY03JTQVyh2ZVsNN_Js0GyPsTPwDJ9Vv4RzqugMl_9LO5roGGKi4teB3kKxX9pPfGIY_ak3ddg4VXEoQeQ4s0Hv9RNTSsMdipx_x771hbOdNcTtj9GsQeEawBLcVzRbzfa_Ghc1QN8UGKHb7SF87jXL4hWPO6mo2s3ZONyDeuuAHK8DV-Mrpl89qjKfuFRt6dHZDDsSqgWfjIlep1qNBEwsx1pm08gS8ZR4sQzsF89Vz5HPmV1Zj4sIA7MVYYua82r1s7SB4r2ZLonkhAw8iWcjiNxRvYCuW2lfT3Dfg");
        Log.d("cLog", "response: " + response2.toString());

        //CompletableFuture<TorusKey> aa = torusSdk.getTorusKey(verifier, verifierId, verifierParamsHashMap, idToken);/*.thenApply((torusKey) -> Pair.create(userInfoArray, torusKey));*/
        CompletableFuture<Object> aa = torusSdk.getTorusKey(verifier, verifierId, verifierParamsHashMap, idToken).thenApply((torusKey) ->
                Pair.create(userInfoArray, torusKey)
        );

        /*   aa.thenApply(torusKey ->
                Triplet.create(userInfo, response2, torusKey)
        );
        aa.thenApplyAsync(triplet ->
                Log.d("cLog", "getPrivateKey:" +triplet.getPrivateKey())
                //triplet.getPrivateKey()
        );*/

        Log.d("","");
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        this.selectedLoginVerifier = (LoginVerifier) adapterView.getSelectedItem();
        //Auth0ClientOptions.Auth0ClientOptionsBuilder builder = null;
        Auth0ClientOptions.Auth0ClientOptionsBuilder builder = new Auth0ClientOptions.Auth0ClientOptionsBuilder(this.selectedLoginVerifier.getDomain());
        builder.setVerifierIdField(this.selectedLoginVerifier.getVerifierIdField());
        builder.setVerifierIdCaseSensitive(this.selectedLoginVerifier.isVerfierIdCaseSensitive());

        //CustomAuth customAuth = new CustomAuth()

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
