package pro.sketchware.core.codegen;

import pro.sketchware.R;
import pro.sketchware.util.Helper;

/** Display names for built-in callbacks; method names and parameter identifiers stay unchanged. */
public final class BuiltinEventLocalization {
    private BuiltinEventLocalization() {}

    public static String getName(String eventName) {
        int resource = switch (eventName) {
            case "onUpdateProfileComplete" -> R.string.event_display_onupdateprofilecomplete;
            case "onEmailVerificationSent" -> R.string.event_display_onemailverificationsent;
            case "onDeleteUserComplete" -> R.string.event_display_ondeleteusercomplete;
            case "onUpdateEmailComplete" -> R.string.event_display_onupdateemailcomplete;
            case "onGoogleSignIn" -> R.string.event_display_ongooglesignin;
            case "onUpdatePasswordComplete" -> R.string.event_display_onupdatepasswordcomplete;
            case "signInWithPhoneAuthComplete" -> R.string.event_display_signinwithphoneauthcomplete;
            case "onScrolled" -> R.string.event_display_onscrolled;
            case "onDateChanged" -> R.string.event_display_ondatechanged;
            case "onDateSet" -> R.string.event_display_ondateset;
            case "onFailureLink" -> R.string.event_display_onfailurelink;
            case "onNavigationItemSelected" -> R.string.event_display_onnavigationitemselected;
            case "onPrepared" -> R.string.event_display_onprepared;
            case "onCompletion" -> R.string.event_display_oncompletion;
            case "onPatternLockCleared" -> R.string.event_display_onpatternlockcleared;
            case "onAccountPickerCancelled" -> R.string.event_display_onaccountpickercancelled;
            case "onPatternLockStarted" -> R.string.event_display_onpatternlockstarted;
            case "onSaveInstanceState" -> R.string.event_display_onsaveinstancestate;
            case "onCreateOptionsMenu" -> R.string.event_display_oncreateoptionsmenu;
            case "onCreateContextMenu" -> R.string.event_display_oncreatecontextmenu;
            case "onRestoreInstanceState" -> R.string.event_display_onrestoreinstancestate;
            case "onContextItemSelected" -> R.string.event_display_oncontextitemselected;
            case "onQueryTextChanged" -> R.string.event_display_onquerytextchanged;
            case "onQueryTextSubmit" -> R.string.event_display_onquerytextsubmit;
            case "onError" -> R.string.event_display_onerror;
            case "onVerificationCompleted" -> R.string.event_display_onverificationcompleted;
            case "onRecyclerScrollChanged" -> R.string.event_display_onrecyclerscrollchanged;
            case "onRecyclerScrolled" -> R.string.event_display_onrecyclerscrolled;
            case "onSuccessLink" -> R.string.event_display_onsuccesslink;
            case "onPageScrolled" -> R.string.event_display_onpagescrolled;
            case "onVerificationFailed" -> R.string.event_display_onverificationfailed;
            case "onAccountPicker" -> R.string.event_display_onaccountpicker;
            case "onTimeSet" -> R.string.event_display_ontimeset;
            case "onPageScrollStateChanged" -> R.string.event_display_onpagescrollstatechanged;
            case "onRatingChanged" -> R.string.event_display_onratingchanged;
            case "onPatternLockProgress" -> R.string.event_display_onpatternlockprogress;
            case "onPatternLockComplete" -> R.string.event_display_onpatternlockcomplete;
            case "onTabSelected" -> R.string.event_display_ontabselected;
            case "onTabUnselected" -> R.string.event_display_ontabunselected;
            case "onTabReselected" -> R.string.event_display_ontabreselected;
            case "onPageSelected" -> R.string.event_display_onpageselected;
            case "onLetterSelected" -> R.string.event_display_onletterselected;
            case "onCompleteRegister" -> R.string.event_display_oncompleteregister;
            case "onCodeSent" -> R.string.event_display_oncodesent;
            case "onTimeChanged" -> R.string.event_display_ontimechanged;
            case "onScrollChanged" -> R.string.event_display_onscrollchanged;
            case "onBannerAdFailedToLoad" -> R.string.event_display_onbanneradfailedtoload;
            case "onInterstitialAdFailedToLoad" -> R.string.event_display_oninterstitialadfailedtoload;
            case "onRewardAdFailedToLoad" -> R.string.event_display_onrewardadfailedtoload;
            case "onAdFailedToShowFullScreenContent" -> R.string.event_display_onadfailedtoshowfullscreencontent;
            case "onUserEarnedReward" -> R.string.event_display_onuserearnedreward;
            case "onAdLoaded" -> R.string.event_display_onadloaded;
            case "onAdDismissedFullScreenContent" -> R.string.event_display_onaddismissedfullscreencontent;
            case "onAdShowedFullScreenContent" -> R.string.event_display_onadshowedfullscreencontent;
            case "onRewardAdLoaded" -> R.string.event_display_onrewardadloaded;
            case "onAdOpened" -> R.string.event_display_onadopened;
            case "onAdClicked" -> R.string.event_display_onadclicked;
            case "onAdClosed" -> R.string.event_display_onadclosed;
            case "onSQLiteError" -> R.string.event_display_onsqliteerror;
            default -> 0;
        };
        return resource == 0 ? eventName : Helper.getResString(resource);
    }
}
