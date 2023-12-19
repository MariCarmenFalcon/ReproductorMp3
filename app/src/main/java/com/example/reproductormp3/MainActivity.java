package com.example.reproductormp3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<String> nombreCanciones = new ArrayList<>();
    private List<String> directorio = new ArrayList<>();
    private static final int REQUEST_PERMISSION = 1;
    private ListView lv;
    private DrawerLayout drawer;
    private ImageView atras, play_pausa, siguiente, stop;
    private int currentSong = 0;
    private final Handler handler = new Handler();
    private  SeekBar seekBar;
    private Runnable updateSeekBar;
    private boolean isSeekBarUpdating = false;
    private TextView textNombreCancion;
    private int posicionActual = 0;
    private boolean bucleActivado = false;
    private ImageView bucle;
    private boolean aleatorioActivado = false;
    private ImageView aleatorio;
    //Agrego nueva lista para orden aleatorio
    private List<Integer> ordenAleatorio = new ArrayList<>();
    MediaPlayer mediaPlayer = new MediaPlayer();
    private TextView textViewTiempoTranscurrido;
    private TextView textViewTiempoRestante;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv = findViewById(R.id.listView);
        drawer = findViewById(R.id.drawerLayout);
        atras = findViewById(R.id.atras);
        play_pausa = findViewById(R.id.play_pausa);
        siguiente = findViewById(R.id.siguiente);
        stop = findViewById(R.id.stop);
        seekBar = findViewById(R.id.seekBar);
        textNombreCancion = findViewById(R.id.TextNombCancion);
        bucle = findViewById(R.id.bucle);
        aleatorio = findViewById(R.id.aleatorio);
        textViewTiempoTranscurrido = findViewById(R.id.textViewTiempoTranscurrido);
        textViewTiempoRestante = findViewById(R.id.textViewTiempoRestante);

        Log.d("ReproductorMP3", "TextView inicializado: " + (textNombreCancion != null));

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);

        } else {

            obtenerCanciones();
            Collections.shuffle(ordenAleatorio);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombreCanciones);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    do {
                        mediaPlayer.reset();
                    } while (mediaPlayer.isPlaying());
                    mediaPlayer.setDataSource(directorio.get(i));
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    currentSong = i;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Si el bucle está activado, vuelve a reproducir la misma canción
                if (bucleActivado) {
                    reproducirCancion(currentSong);
                } else {
                    // Reproduce la siguiente canción en la lista original
                    reproducirCancion(currentSong + 1);
                }
            }
        });

        play_pausa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer.isPlaying()) {
                    // Almacena la posición actual antes de pausar
                    posicionActual = mediaPlayer.getCurrentPosition();
                    mediaPlayer.pause();
                    // Cambia la imagen del botón a "play"
                    play_pausa.setImageResource(R.drawable.play);
                    isSeekBarUpdating = false;

                } else {
                    try {
                        do {
                            mediaPlayer.reset();
                        } while (mediaPlayer.isPlaying());
                        mediaPlayer.setDataSource(directorio.get(0));
                        mediaPlayer.prepare();
                        seekBar.setMax(mediaPlayer.getDuration());

                        // Restaura la posición actual antes de iniciar la reproducción
                        mediaPlayer.seekTo(posicionActual);

                        mediaPlayer.start();
                        currentSong = 0;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Cambia la imagen del botón a "pausa"
                    play_pausa.setImageResource(R.drawable.pausa);
                    isSeekBarUpdating = true;
                    handler.postDelayed(updateSeekBar, 100);
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer.isPlaying()) {
                    // Almacena la posición actual antes de pausar
                    posicionActual = mediaPlayer.getCurrentPosition();
                    mediaPlayer.pause();
                    // Mueve la reproducción al principio
                    mediaPlayer.seekTo(0);
                    // Reinicia la posición de la SeekBar
                    seekBar.setProgress(0);
                    isSeekBarUpdating = false;

                    // Actualiza los TextViews Inicio y Final
                    actualizarSeekBar();

                    // Cambia la imagen del botón a "play"
                    play_pausa.setImageResource(R.drawable.play);
                }
            }
        });

        atras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (currentSong > 0) {
                    // Si hay canciones anteriores, reproduce la anterior
                    reproducirCancion(currentSong - 1);
                } else {
                    // Estoy en la primera canción, reproduce la última
                    reproducirCancion(nombreCanciones.size() - 1);
                }
            }
        });

        siguiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (currentSong < nombreCanciones.size() - 1) {
                    // Si hay canciones siguientes, reproduce la siguiente
                    reproducirCancion(currentSong + 1);
                } else {
                    // Estoy en la última canción, reproduce la primera
                    reproducirCancion(0);
                }
            }
        });

        bucle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bucleActivado = !bucleActivado;

                // Muestra un mensaje Toast indicando el estado del bucle
                String mensaje = bucleActivado ? "Bucle activado" : "Bucle desactivado";
                Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT).show();

                // Si el bucle está activado, configura el bucle en el MediaPlayer
                if (bucleActivado) {
                    mediaPlayer.setLooping(true);
                } else {
                    // Si el bucle se desactiva, detén la reproducción continua
                    mediaPlayer.setLooping(false);
                }
            }
        });

        aleatorio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                aleatorioActivado = !aleatorioActivado;

                // Muestra un mensaje Toast indicando el estado del modo aleatorio
                String mensaje = aleatorioActivado ? "Modo aleatorio activado" : "Modo aleatorio desactivado";
                Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT * 3).show();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Si el cambio de progreso es causado por el usuario, actualiza la posición de reproducción
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Se llama cuando se toca la SeekBar, puedes detener la actualización del SeekBar aquí
                isSeekBarUpdating = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Se llama cuando se deja de tocar la SeekBar, puedes reanudar la actualización del SeekBar aquí
                isSeekBarUpdating = true;
                handler.postDelayed(updateSeekBar, 100);
            }
        });

        // Actualiza el SeekBar solo si isSeekBarUpdating es verdadero
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isSeekBarUpdating) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);

                    // Actualiza el tiempo transcurrido y restante
                    int tiempoTranscurrido = currentPosition;
                    int tiempoRestante = mediaPlayer.getDuration() - tiempoTranscurrido;

                    // Convierte los tiempos a formato MM:SS
                    String tiempoTranscurridoStr = formatTime(tiempoTranscurrido);
                    String tiempoRestanteStr = formatTime(tiempoRestante);

                    // Actualiza los TextViews
                    textViewTiempoTranscurrido.setText(tiempoTranscurridoStr);
                    textViewTiempoRestante.setText(tiempoRestanteStr);

                    handler.postDelayed(this, 100);
                }
            }
        };
    }

    public void botonDrawer(View view){
        drawer.openDrawer(GravityCompat.START);
    }

    public void obtenerCanciones(){

        //Ruta al directorio de almacenamiento externo
        File directorioDCIM = new File((Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()));

        //Verifica si el directorio existe
        if (directorioDCIM.exists()) {

            // Obtener una lista de archivos en el directorio RAIZ DEL DISPOSITIVO
            File[] archivos = directorioDCIM.listFiles();

            for (int i = 0; i < archivos.length; i++) {

                // Verifica si el archivo es de tipo audio
                if (archivos[i].getName().endsWith(".mp3") || archivos[i].getName().endsWith(".wav") || archivos[i].getName().endsWith(".ogg")) {
                    nombreCanciones.add(archivos[i].getName());
                    directorio.add(archivos[i].getAbsolutePath());
                    Log.d("nombre canción", nombreCanciones.get(i));
                    Log.d("ruta canción", directorio.get(i));
                    Log.d("ReproductorMP3", "Nombre de la canción: " + archivos[i].getName());
                }
            }
        }
    }
    private void reproducirCancion(int index) {

        // Muestra el nombre de la canción en el TextView
        textNombreCancion.setText(nombreCanciones.get(index));

        try {
            do {
                mediaPlayer.reset();
            } while (mediaPlayer.isPlaying());

            if (aleatorioActivado) {
                index = ordenAleatorio.get(index);
            }
                mediaPlayer.setDataSource(directorio.get(index));
                mediaPlayer.prepare();
                mediaPlayer.start();

            // Actualiza el índice de la canción actual
            currentSong = index;

            // Configura el bucle según el estado actual
            mediaPlayer.setLooping(bucleActivado);

            // Actualiza el SeekBar
            actualizarSeekBar();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void actualizarSeekBar() {

        if (mediaPlayer != null && isSeekBarUpdating) {
            int tiempoTranscurrido = mediaPlayer.getCurrentPosition();
            int tiempoRestante = mediaPlayer.getDuration() - tiempoTranscurrido;

            // Actualiza el tiempo transcurrido y restante en los TextViews
            textViewTiempoTranscurrido.setText(formatTime(tiempoTranscurrido));
            textViewTiempoRestante.setText(formatTime(tiempoRestante));

            // Obtengo la duración total de la canción
            int duracionTotal = mediaPlayer.getDuration();

            // Configuro el max del SeekBar con la duración total
            seekBar.setMax(duracionTotal);

            // Obtengo el nombre de la canción actual
            String nombreCancionActual = nombreCanciones.get(currentSong);

            // Actualizo el TextView con el nombre de la canción actual
            textNombreCancion.setText(nombreCancionActual);

            // Actualiza el progreso del SeekBar
            seekBar.setProgress(tiempoTranscurrido);

            // Actualiza el indicador de tiempo restante en el SeekBar
            seekBar.setSecondaryProgress(tiempoTranscurrido + 5000); // Indicador de tiempo restante

            // Programa la próxima actualización del SeekBar
            handler.postDelayed(updateSeekBar, 100);
        }
    }

    // Función para convertir milisegundos a formato "mm:ss"
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = ((milliseconds / 1000) / 60) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }


}
