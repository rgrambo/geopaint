package edu.uw.rgrambo.geopaint;

        import android.os.Bundle;
        import android.preference.PreferenceFragment;

public class ColorPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set the background color to be gray
        getView().setBackgroundColor(getActivity().getResources().getColor(R.color.gray));
        getView().setClickable(true);
    }
}