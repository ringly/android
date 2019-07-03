package com.ringly.ringly.ui.screens;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Optional;
import com.ringly.ringly.R;
import com.ringly.ringly.config.Color;
import com.ringly.ringly.ui.MainActivity;


public final class ContactsFragment extends Fragment {
    private static final String TAG = ContactsFragment.class.getCanonicalName();

    private static final String[] CONTACT_COLUMNS = {ContactsContract.Contacts.DISPLAY_NAME};
    private static final char URI_PATH_SEPARATOR = '/';

    private static final int FILL_CONTACTS_REQ_ID = 1;
    private static final int ADD_CONTACT_REQ_ID = 2;

    private RecyclerView mList;
    private MainActivity mActivity;
    private ContactsAdapter mAdapter;


    ////
    //// Fragment methods
    ////

    @Override
    public void onAttach(final Activity activity) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        mActivity = (MainActivity) activity;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate"); // NON-NLS
        super.onCreate(savedInstanceState);

        mAdapter = new ContactsAdapter(mActivity);
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState
    ) {
        final View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        setHasOptionsMenu(true);

        mActivity.onCreateView(view);

        mList = (RecyclerView) view.findViewById(R.id.list);
        mList.setHasFixedSize(true);
        mList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mList.setAdapter(mAdapter);

        fillContacts();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contacts, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_add:
                startAddContact();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onActivityResult: " + data);

        if (resultCode == Activity.RESULT_OK) {
            // Keep lookup key to use in the future
            Uri uri = data.getData();
            String[] projection = { ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY };
            Cursor cursor = getContext().getContentResolver()
                .query(uri, projection, null, null, null);
            cursor.moveToFirst();

            String contact =
                cursor.getString(cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
                ));

            cursor.close();

            if (!mActivity.getPreferences().getContactColor(contact).isPresent()) {
                mActivity.getPreferences().setContactColor(contact, Color.NONE);
            }

            addContact(contact, true); // LOOKUP_KEY
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If we got the permission, try the operation again.
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case FILL_CONTACTS_REQ_ID:
                    fillContacts();
                    break;
                case ADD_CONTACT_REQ_ID:
                    startAddContact();
                    break;
            }
        }
}

    @Override
    public void onDestroy() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDestroy");

        mAdapter.flush();

        super.onDestroy();
    }


    //
    // private methods
    //

    private boolean checkContactsPermission(int reqId) {
        if(ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_CONTACTS) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, reqId);
            return false;
        }

        return true;
    }

    private void fillContacts() {
        if(mActivity.getPreferences().getContacts().size() > 0
                && checkContactsPermission(FILL_CONTACTS_REQ_ID)) {
            for (final String contact : mActivity.getPreferences().getContacts()) {
                addContact(contact, false); // Note: doesn't add if already there
            }
        }
    }

    private void startAddContact() {
        if(checkContactsPermission(ADD_CONTACT_REQ_ID)) {
            Intent i = new Intent(
                    Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            i.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            startActivityForResult(i, 0);
        }
    }

    private void addContact(final String contact, final boolean scroll) {
        final Optional<String> name = getName(contact);

        if (name.isPresent()) {
            final int i = mAdapter.add(contact, name.get());
            if (scroll) mList.smoothScrollToPosition(i);
        } else { // contact no longer exists, or has no display name
            //noinspection HardCodedStringLiteral
            Log.i(TAG, "couldn't find contact " + contact);

            mActivity.getPreferences().removeContact(contact);
        }
    }

    private Optional<String> getName(final String contact) {
        // TODO use "complete" lookup URI with last known _ID
        final Uri uri = ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon()
                .appendEncodedPath(contact).build();
        final Cursor cursor = getActivity().getContentResolver()
                .query(uri, CONTACT_COLUMNS, null, null, null);
        // TODO once we drop Android 4.3 we can use try-with-resources syntax
        //noinspection TryFinallyCanBeTryWithResources
        try {
            if (cursor.moveToNext()) return Optional.fromNullable(cursor.getString(0));
            else return Optional.absent();
        } finally {
            cursor.close();
        }
    }
}
