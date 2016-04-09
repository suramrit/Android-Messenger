package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {



    static final Uri CONTENT_URI= buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
    // use SQLite DB to store data
    private SQLiteDatabase db;




    private static Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //
        Context context = getContext();
        Set<Map.Entry<String,Object>> s=values.valueSet();
        Iterator itr = s.iterator();
        String val = new String();
        String key = new String();
        String KEY_FIELD = "key";
        String VALUE_FIELD = "value";
        int i = 0;
        while(itr.hasNext())
        {
            Map.Entry me = (Map.Entry)itr.next();
            // me = (Map.Entry)itr.next();
            //String key = me.getKey().toString();
            Object value =  me.getValue();

            //System.out.println("Values are - - -" +", values:"+(String)(value == null?null:value.toString()));
            if(i==0){
                val = value.toString();
                i++;
            }
            else
            {
                key = value.toString();
            }
        }
        ContentValues val_insrt = new ContentValues();
        val_insrt.put(VALUE_FIELD,val);
        val_insrt.put(KEY_FIELD, key);


        System.out.println("Extracted Values:key-- " + key + " val:" + val);
        String filename = key;
        FileOutputStream outputStream;

        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(val.getBytes());
            outputStream.close();
        }
        catch(Exception e){
            System.out.println("FILE WRITE FAILED");
        }

        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */




        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        Context context = getContext();
        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /* TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key","value"});
        Context context = getContext();
        System.out.println("Key recvd is: "+ selection);
        // query is implemented as: mContentResolver.query(mUri, null, key, null, null);
        //read the file using 'key' and then add a row to cursor and return
        //then check -- DONE
        String filename = selection;
        FileInputStream inputStream ;

        try {
            // InputStream inputStream = new BufferedInputStream(new FileInputStream(filename),1024);



            inputStream = context.openFileInput(filename);
            int avail = inputStream.available();
            byte[] value = new byte[avail];
            inputStream.read(value);
            inputStream.close();
            String read_val = new String(value, "UTF-8");
            //reader.close();
            System.out.println("The value read is:" +read_val );
            //Can correctly read from file: Check how to send it as cursor
            Object[] ret_cursor = {filename,read_val};
            matrixCursor.addRow(ret_cursor);
        }
        catch(Exception e){
            System.out.println("FILE OPEN FAILED");
        }
        Log.v("query", selection);
        return matrixCursor;
    }
}
