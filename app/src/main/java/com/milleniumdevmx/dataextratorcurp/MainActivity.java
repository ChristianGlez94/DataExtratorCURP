package com.milleniumdevmx.dataextratorcurp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    EditText nombreEditText, apellidoPaternoEditText, apellidoMaternoEditText,curpEditText, correoEditText, contrasenaEditText, verificarContrasenaEditText, telefonoEditText, editTextPIN;
    Button registerButton;
    String verificationCode;
    String savedNombre, savedApellidoPaterno, savedApellidoMaterno,savedCurp, savedCorreo, savedContrasena, savedTelefono;
    boolean isVerificationMode = false; // Track if we're in verification mode
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 102;
    private ImageButton buttonScanQR, buttonpdf;
    private TextView textViewQRResult,curpTextView;
    private ProgressBar progressBar;
    private static final int PICK_PDF_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Initialize views
        nombreEditText = findViewById(R.id.nombreEditText);
        apellidoPaternoEditText = findViewById(R.id.apellidoPaternoEditText);
        apellidoMaternoEditText = findViewById(R.id.apellidoMaternoEditText);
        curpEditText=findViewById(R.id.curpEditText);
        correoEditText = findViewById(R.id.correoEditText);
        curpTextView=findViewById(R.id.textView24);
        contrasenaEditText = findViewById(R.id.contrasenaEditText);
        verificarContrasenaEditText = findViewById(R.id.contrasenaEditText2);
        telefonoEditText = findViewById(R.id.telefonoEditText);
        editTextPIN = findViewById(R.id.editTextPIN); // Verification PIN field
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar2);
        buttonpdf=findViewById(R.id.buttonuploadpdf);


        registerButton.setOnClickListener(v -> {
            if (isVerificationMode) {


            } else {


            }
        });

        buttonScanQR = findViewById(R.id.buttonScanQR);

        buttonScanQR.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startQRScanner();
            } else {
                requestCameraPermission();
            }
        });


        // Solicitar permisos de almacenamiento
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        buttonpdf.setOnClickListener(v -> selectPdfFile());

        curpTextView.setOnClickListener(v ->abrirweb());


    }


    private void abrirweb() {
        String url = "https://www.gob.mx/curp/";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    private void selectPdfFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Selecciona un archivo PDF"), PICK_PDF_FILE);
    }



    private void extractTextFromPdf(InputStream inputStream) {
        try {
            // Cargar el PDF en iText
            PdfReader pdfReader = new PdfReader(inputStream);

            StringBuilder extractedText = new StringBuilder();
            int numberOfPages = pdfReader.getNumberOfPages();

            // Extraer texto de todas las páginas
            for (int i = 1; i <= numberOfPages; i++) {
                extractedText.append(PdfTextExtractor.getTextFromPage(pdfReader, i));
            }

            pdfReader.close();

            // Buscar CURP y Nombre
            findCurpAndName(extractedText.toString());

        } catch (Exception e) {
            Log.e("PDF Extraction", "Error al extraer texto del PDF", e);
        }
    }

    private void findCurpAndName(String text) {
        // Buscar el CURP usando una expresión regular
        String curpPattern = "\\b[A-Z0-9]{16,18}\\b"; // CURP estándar
        java.util.regex.Pattern patternCurp = java.util.regex.Pattern.compile(curpPattern);
        java.util.regex.Matcher matcherCurp = patternCurp.matcher(text);

        // Buscar el Nombre completo (en formato de actas de nacimiento)
        String namePattern = "\\b[A-ZÁÉÍÓÚÑ]{2,}( [A-ZÁÉÍÓÚÑ]{2,}){2,}\\b"; // Nombre completo en mayúsculas
        java.util.regex.Pattern patternName = java.util.regex.Pattern.compile(namePattern);
        java.util.regex.Matcher matcherName = patternName.matcher(text);

        // Mostrar el CURP en el TextView
        if (matcherCurp.find()) {
            curpEditText.setText(matcherCurp.group());
        } else {
            Toast.makeText(this, "No se encontró CURP", Toast.LENGTH_SHORT).show();
        }

        // Procesar el Nombre completo
        if (matcherName.find()) {
            String fullName = matcherName.group();
            String[] nameParts = fullName.split(" ");

            if (nameParts.length >= 3) {
                apellidoPaternoEditText.setText(nameParts[1]);
                apellidoMaternoEditText.setText(nameParts[2]);
                nombreEditText.setText(nameParts[0]);
            } else {
                Toast.makeText(this, "El nombre no tiene el formato esperado", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No se encontró un nombre válido", Toast.LENGTH_SHORT).show();
        }
    }




    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, "La cámara es necesaria para escanear códigos QR", Toast.LENGTH_SHORT).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Escanea tu CURP");
        integrator.setCameraId(0); // Cámara trasera
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.setCaptureActivity(CustomScannerActivity.class); // Usa la actividad personalizada
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                extractData(result.getContents());
            } else {
                Toast.makeText(this, "No se escaneó ningún código", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == PICK_PDF_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                extractTextFromPdf(inputStream);
            } catch (Exception e) {
                Log.e("PDF Extraction", "Error al procesar el archivo PDF", e);
            }
        }
    }

    private void extractData(String scannedText) {
        try {
            // Dividir el texto escaneado en partes
            String[] parts = scannedText.split("\\|");

            // Asegurarse de que el formato sea válido
            if (parts.length >= 6) {
                String curp = parts[0]; // CURP
                String apellidoPaternocurp = parts[2]; // Apellido Paterno
                String apellidoMaternocurp = parts[3]; // Apellido Materno
                String nombre = parts[4]; // Nombre

                // Mostrar los datos en los TextViews
                apellidoPaternoEditText.setText(apellidoPaternocurp);
                apellidoMaternoEditText.setText(apellidoMaternocurp);
                nombreEditText.setText(nombre);
                curpEditText.setText(curp);
            } else {
                Toast.makeText(this, "Formato del código no válido", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al procesar los datos", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRScanner();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
