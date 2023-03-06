package chatcliente;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class Cliente extends Thread {
    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private final VentanaC ventana;
    private String identificador;
    private boolean escuchando;
    private final String host;
    private final int puerto;
    private boolean grabando;
    private SourceDataLine lineaSalida;
    private boolean transmitiendoVideo;
    private Dimension dimension;

    Cliente(VentanaC ventana, String host, Integer puerto, String nombre) {
        this.ventana = ventana;
        this.host = host;
        this.puerto = puerto;
        this.identificador = nombre;
        escuchando = true;
        grabando = false;
        transmitiendoVideo = false;
        this.start();
    }

    public void run() {
        try {
            socket = new Socket(host, puerto);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            System.out.println("Conexion exitosa!!!!");
            this.enviarSolicitudConexion(identificador);
            this.escuchar();
        } catch (UnknownHostException ex) {
            JOptionPane.showMessageDialog(ventana,
                    "Conexión rehusada, servidor desconocido,\n"
                            + "puede que haya ingresado una ip incorrecta\n"
                            + "o que el servidor no este corriendo.\n"
                            + "Esta aplicación se cerrará.");
            System.exit(0);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(ventana,
                    "Conexión rehusada, error de Entrada/Salida,\n"
                            + "puede que haya ingresado una ip o un puerto\n"
                            + "incorrecto, o que el servidor no este corriendo.\n"
                            + "Esta aplicación se cerrará.");
            System.exit(0);
        }
    }

    public void desconectar() {
        try {
            objectOutputStream.close();
            objectInputStream.close();
            socket.close();
            escuchando = false;
            if (grabando) {
                detenerGrabacion();
            }
            if (transmitiendoVideo) {
                detenerTransmisionVideo();
            }
        } catch (Exception e) {
            System.err.println("Error al cerrar los elementos de comunicación del cliente.");
        }
    }

    public void enviarMensaje(String cliente_receptor, String mensaje){
        LinkedList<String> lista=new LinkedList<>();
        lista.add("MENSAJE");
        lista.add(identificador);
        lista.add(cliente_receptor);
        lista.add(mensaje);
        try {
            objectOutputStream.writeObject(lista);
        } catch (IOException ex) {
            System.out.println("Error de lectura y escritura al enviar mensaje al servidor.");
        }

    }

    private void iniciarGrabacion() {
        try {
            AudioFormat formato = getFormatoAudio();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, formato);
            TargetDataLine lineaEntrada = (TargetDataLine) AudioSystem.getLine(info);
            lineaEntrada.open(formato);
            lineaEntrada.start();
            grabando = true;
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (grabando) {
                bytesRead = lineaEntrada.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, InetAddress.getByName(host), puerto);
                    DatagramSocket socketUDP = new DatagramSocket();
                    DatagramPacket paquete = new DatagramPacket
                    (buffer, bytesRead, InetAddress.getByName(host), puerto);
                    socketUDP.send(paquete);
                }
            }
            lineaEntrada.stop();
            lineaEntrada.close();
        } catch (LineUnavailableException | IOException ex) {
            System.err.println("Error al iniciar la grabación de audio del cliente.");
        }
    }
        private void detenerGrabacion() {
            grabando = false;
        }
        private AudioFormat getFormatoAudio() {
            float sampleRate = 16000.0F;
            int sampleSizeInBits = 16;
            int channels = 1;
            boolean signed = true;
            boolean bigEndian = false;
            return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        }
        public void enviarVideo() {
        try {
            VideoCapture capturadorVideo = new VideoCapture();
            capturadorVideo.open(0);
            while (transmitiendoVideo) {
                Mat frame = new Mat();
                capturadorVideo.read(frame);
                BufferedImage image = Mat2BufferedImage(frame);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", byteArrayOutputStream);
                byte[] bytes = byteArrayOutputStream.toByteArray();
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(host), puerto + 1);
                DatagramSocket datagramSocket = new DatagramSocket();
                datagramSocket.send(packet);
                datagramSocket.close();
            }
            capturadorVideo.release();
        } catch (Exception e) {
            System.err.println("Error al transmitir video.");
        }
    }
        public void detenerTransmisionVideo() {
    transmitiendoVideo=false;
    try {
        objectOutputStream.writeObject("FIN_TRANSMISION_VIDEO");
    } catch (IOException e) {
        System.out.println("Error al enviar solicitud de fin de transmisión de video al servidor.");
    }
}
        
        public void escuchar() {
            try {
                while (escuchando) {
                    Object aux = objectInputStream.readObject();
                    if (aux != null) {
                        if (aux instanceof LinkedList) {
                            ejecutar((LinkedList<String>)aux);
                        } else {
                            System.err.println("Se recibió un Objeto desconocido a través del socket");
                        }
                    } else {
                        System.err.println("Se recibió un null a través del socket");
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(ventana, 
                        "La comunicación con el servidor se ha\n"                                                 
                                + "perdido, este chat tendrá que finalizar.\n"                                                 
                                + "Esta aplicación se cerrará.");
                System.exit(0);
            }
        }

    public void ejecutar(LinkedList<String> lista){
        String tipo=lista.get(0);
        switch (tipo) {
            case "CONEXION_ACEPTADA":
                identificador=lista.get(1);
                ventana.sesionIniciada(identificador);
                for(int i=2;i<lista.size();i++){
                    ventana.addContacto(lista.get(i));
                }
                break;
            case "NUEVO_USUARIO_CONECTADO":
                ventana.addContacto(lista.get(1));
                break;
            case "USUARIO_DESCONECTADO":
                ventana.eliminarContacto(lista.get(1));
                break;                
            case "MENSAJE":
                ventana.addMensaje(lista.get(1), lista.get(3));
                break;
            default:
                break;
            case "GRABAR":
                iniciarGrabacion();
                break;
            case "DETENER_GRABACION":
                detenerGrabacion();
               break;
        }
    }

    private void enviarSolicitudConexion(String identificador) {
        LinkedList<String> lista=new LinkedList<>();
        lista.add("SOLICITUD_CONEXION");
        lista.add(identificador);
        try {
            objectOutputStream.writeObject(lista);
        } catch (IOException ex) {
            System.out.println("Error de lectura y escritura al enviar mensaje al servidor.");
        }
    }

    void confirmarDesconexion() {
        LinkedList<String> lista=new LinkedList<>();
        lista.add("SOLICITUD_DESCONEXION");
        lista.add(identificador);
        try {
            objectOutputStream.writeObject(lista);
        } catch (IOException ex) {
            System.out.println("Error de lectura y escritura al enviar mensaje al servidor.");
        }
    }

    String getIdentificador() {
        return identificador;
    }

    private BufferedImage Mat2BufferedImage(Mat frame) {
    // Preparar la imagen para ser mostrada en un objeto BufferedImage
    int tipo = BufferedImage.TYPE_BYTE_GRAY;
    if (frame.channels() > 1) {
        tipo = BufferedImage.TYPE_3BYTE_BGR;
    }
    int ancho = frame.width();
    int alto = frame.height();
    BufferedImage imagen = new BufferedImage(ancho, alto, tipo);
    WritableRaster raster = imagen.getRaster();
    DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
    byte[] data = dataBuffer.getData();
    frame.get(0, 0, data);
    return imagen;
}
}