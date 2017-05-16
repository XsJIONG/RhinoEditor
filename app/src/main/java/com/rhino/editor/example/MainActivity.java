package com.rhino.editor.example;

import android.app.Activity;
import android.os.Bundle;
import com.rhino.editor.example.R;
import com.rhino.editor.RhinoEditor;
import com.rhino.editor.example.MainActivity;

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
