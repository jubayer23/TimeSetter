package com.morydes.rideshare;

import android.app.Activity;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;

import com.morydes.rideshare.alertbanner.AlertDialogForAnything;
import com.morydes.rideshare.billingUtil.IabBroadcastReceiver;
import com.morydes.rideshare.billingUtil.IabHelper;
import com.morydes.rideshare.billingUtil.IabResult;
import com.morydes.rideshare.billingUtil.Inventory;
import com.morydes.rideshare.billingUtil.Purchase;

/**
 * Created by jubayer on 4/19/2018.
 */

public class BillingHelper {
    // Debug tag, for logging
    static final String TAG = "DebugBilling";
    // The helper object
    IabHelper mHelper;

    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;

    private Activity activity;

    // Does the user have the premium upgrade?
   private boolean mIsPremium = false;

    /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
 * (that you got from the Google Play developer console). This is not your
 * developer public key, it's the *app-specific* public key.
 *
 * Instead of just storing the entire literal string here embedded in the
 * program,  construct the key at runtime from pieces or
 * use bit manipulation (for creative, XOR with some other string) to hide
 * the actual key.  The key itself is not secret information, but we don't
 * want to make it easy for an attacker to replace the public key with one
 * of their own and then fake messages from the server.
 */
    String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAthXx72yUbA+MDl4BWSkELZ3h3uIQ5bPQoVH08o4C2Qu6gJVw+XGbBf1F8HoREONLTg4OpWs6w+UvnUZHmeKgm5Zf0HXKTFaQDqE57X/n3Pchhc+kJB9vOMvPdPWE+TEfuNnLg8o1urHn1mbMCc6c60n870MS1aDc8fNRYgMc5iW2yGuVeZ5jTj7ywy6A/cTaK/MrsEEiEsXygQety8KSIi0bnLwdkXcVctXgs3TUtWrN/YGcD/dnknIaXWcdrjCsnML5GxgRjTxKP82Q3PgmoWYUY/ekXx+mlqTydA6V+IcOOM+yWsBbZS5s0XMnvRx+W5NTxkC81lB9JRk9gc77bQIDAQAB";
    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    static final String SKU_PREMIUM = "premium";

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    public BillingHelper(Activity activity){

        this.activity = activity;
    }

    public void initiateBillingConfiguration(){
        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(activity, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    //complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(((MainActivity) activity));
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                activity.registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    //complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            //complain("Error querying inventory. Another async operation in progress.");
        }
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                //complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));



            //updateUi();
            setWaitScreen(false);
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };


    // User clicked the "Upgrade to Premium" button.
    public void onUpgradeAppButtonClicked() {
        Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
        setWaitScreen(true);



        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";

        try {
            mHelper.launchPurchaseFlow(activity, SKU_PREMIUM, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            //complain("Error launching purchase flow. Another async operation in progress.");
            setWaitScreen(false);
        }
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                //complain("Error purchasing: " + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
               // complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                //alert("Thank you for upgrading to premium!");
                mIsPremium = true;
               // updateUi();
                setWaitScreen(false);
                AlertDialogForAnything.showNotifyDialog(activity,AlertDialogForAnything.ALERT_TYPE_SUCCESS,"You successfully subscribed!");
            }
        }
    };


    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }


    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
        if(true){
            ((MainActivity) activity).showProgressDialog("please wait..",true,false);
        }else{
            ((MainActivity) activity).dismissProgressDialog();
        }
    }

    public IabHelper getmHelper(){
        return mHelper;
    }

    public boolean getIsPremium(){
        return mIsPremium;
    }
}
