package com.rhino.editor;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity 
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		RhinoEditor.create(MainActivity.this);
    }
}
