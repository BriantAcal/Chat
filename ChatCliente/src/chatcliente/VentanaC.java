package chatcliente;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import org.opencv.videoio.Videoio;


public class VentanaC extends javax.swing.JFrame {
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private String identificador;
    private boolean escuchando;
    private boolean grabando;
    private SourceDataLine lineaSalida;
    private String host;
    private int puerto;
    private File audioFile;
    private TargetDataLine targetDataLine;
    private boolean transmitiendoVideo;
    private Dimension dimension;
    private VideoCapture capturador;
    private BufferedImage imagenVideo;
    private JPanel panelVideo;
    private boolean camaraEncendida = false;

    private void iniciarGrabacion() {
    try {
        AudioFormat formatoAudio = getFormatoAudio();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, formatoAudio);
        targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
        targetDataLine.open(formatoAudio);
        targetDataLine.start();
        audioFile = File.createTempFile("temp", ".wav");
        Thread grabacionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioSystem.write(new AudioInputStream(targetDataLine), AudioFileFormat.Type.WAVE, audioFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        grabacionThread.start();
    } catch (LineUnavailableException | IOException e) {
        e.printStackTrace();
    }
}

    private void detenerGrabacion() {
        targetDataLine.stop();
        targetDataLine.close();
    }
    private void enviarAudio() {
        try {
            AudioFormat formatoAudio = getFormatoAudio();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, formatoAudio);
            lineaSalida = (SourceDataLine) AudioSystem.getLine(info);
            lineaSalida.open(formatoAudio);
            lineaSalida.start();
            byte[] tempBuffer = new byte[10000];
            DatagramSocket socket = new DatagramSocket();
            InetAddress direccionDestino = InetAddress.getByName(host);
            int puertoDestino = puerto + 1;
            DatagramPacket paquete = new DatagramPacket(tempBuffer, tempBuffer.length, direccionDestino, puertoDestino);
            targetDataLine.start();
            while (grabando) {
                int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                if (cnt > 0) {
                lineaSalida.write(tempBuffer, 0, cnt);
                paquete.setData(tempBuffer);
                paquete.setLength(cnt);
                socket.send(paquete);
            }
        }
        lineaSalida.drain();
        lineaSalida.close();
        socket.close();
    } catch (LineUnavailableException | IOException e) {
        e.printStackTrace();
    }
}
    private AudioFormat getFormatoAudio() {
    float sampleRate = 16000.0F;
    int sampleSizeInBits = 16;
    int channels = 1;
    boolean signed = true;
    boolean bigEndian = false;
    return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
}

    private void encenderCamara() {
    System.setProperty("java.library.path", "C:\\Program Files\\Opencv-4.6.0\\build\\java\\x64");
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    capturador = new VideoCapture(0);
    if (capturador.isOpened()) {
        Thread hiloVideo = new Thread(new Runnable() {
            @Override
            public void run() {
                Mat imagenMat = new Mat();
                while (true) {
                    if (capturador.read(imagenMat)) {
                        imagenVideo = matToBufferedImage(imagenMat);
                        System.out.println("Imagen obtenida de la cámara");
                        if (panelVideo != null) {
                            panelVideo.repaint();
                        }
                        while (transmitiendoVideo) {
                            try {
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                ImageIO.write(imagenVideo, "jpg", byteArrayOutputStream);
                                byte[] imageData = byteArrayOutputStream.toByteArray();

                                DatagramSocket socket = new DatagramSocket();
                                InetAddress direccionDestino = InetAddress.getByName(host);
                                int puertoDestino = puerto + 2;
                                DatagramPacket paquete = new DatagramPacket(imageData, imageData.length,
                                        direccionDestino, puertoDestino);
                                socket.send(paquete);
                                socket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        hiloVideo.start();
        camaraEncendida = true;
    }
}


    private void apagarCamara() {
        if (camaraEncendida) {
            capturador.release();
            camaraEncendida = false;
        }
    }


private BufferedImage matToBufferedImage(Mat matriz) {
    int tipoImagen = BufferedImage.TYPE_BYTE_GRAY;
    if (matriz.channels() > 1) {
        tipoImagen = BufferedImage.TYPE_3BYTE_BGR;
    }
    int ancho = matriz.width();
    int alto = matriz.height();
    BufferedImage imagen = new BufferedImage(ancho, alto, tipoImagen);
    byte[] bytesImagen = new byte[ancho * alto * (int) matriz.elemSize()];
    matriz.get(0, 0, bytesImagen);
    imagen.getRaster().setDataElements(0, 0, ancho, alto, bytesImagen);
    return imagen;
}
    public VentanaC() {
        initComponents();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        String ip_puerto_nombre[] = getIP_Puerto_Nombre();
        String ip = ip_puerto_nombre[0];
        String puerto = ip_puerto_nombre[1];
        String nombre = ip_puerto_nombre[2];
        cliente = new Cliente(this, ip, Integer.valueOf(puerto), nombre);
        JLabel etiqueta = new JLabel("Etiqueta de ejemplo");
        getContentPane().add(etiqueta);
        JPanel panel = new JPanel();
        getContentPane().add(panel);
        System.out.println("Tamaño del panel: " + panel.getSize()); // Imprime el tamaño del panel
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        txtHistorial = new javax.swing.JTextArea();
        txtMensaje = new javax.swing.JTextField();
        cmbContactos = new javax.swing.JComboBox();
        btnEnviar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        txtHistorial.setEditable(false);
        txtHistorial.setBackground(java.awt.Color.white);
        txtHistorial.setColumns(20);
        txtHistorial.setRows(5);
        jScrollPane1.setViewportView(txtHistorial);

        btnEnviar.setBackground(java.awt.Color.gray);
        btnEnviar.setForeground(java.awt.Color.white);
        btnEnviar.setText("Enviar");
        btnEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarActionPerformed(evt);
            }
        });

        jLabel1.setText("Destinatario:");

        jButton1.setText("Micro Encendido");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Micro Apagado");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Camara Encendida");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText("Camara Apagada");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txtMensaje)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEnviar))
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                        .addComponent(cmbContactos, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton3)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jButton4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbContactos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEnviar))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarActionPerformed
        if(cmbContactos.getSelectedItem()==null){
            JOptionPane.showMessageDialog(this, 
                    "Debe escoger un destinatario válido, si no \n"                                                 
                            + "hay uno, espere a que otro usuario se conecte\n"                                                
                            + "para poder chatear con él.");        
            return;
        }
        String cliente_receptor=cmbContactos.getSelectedItem().toString();
        String mensaje=txtMensaje.getText();
        cliente.enviarMensaje(cliente_receptor, mensaje);
        txtHistorial.append("## Yo -> "+cliente_receptor+ " ## : \n" + mensaje+"\n");
        txtMensaje.setText("");
    }//GEN-LAST:event_btnEnviarActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        cliente.confirmarDesconexion();
    }//GEN-LAST:event_formWindowClosing

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    if (!grabando) {
        iniciarGrabacion();
        grabando = true;
        new Thread(new Runnable() {
            public void run() {
                enviarAudio();
            }
        }).start();
    }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        if (grabando) {
        grabando = false;
        detenerGrabacion();
    }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        encenderCamara();   
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        apagarCamara();
    }//GEN-LAST:event_jButton4ActionPerformed

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(VentanaC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(VentanaC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(VentanaC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VentanaC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VentanaC().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEnviar;
    private javax.swing.JComboBox cmbContactos;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea txtHistorial;
    private javax.swing.JTextField txtMensaje;
    // End of variables declaration//GEN-END:variables

    private final String DEFAULT_PORT="8000";
    private final String DEFAULT_IP="127.0.0.1";
    private final Cliente cliente;
    void addContacto(String contacto) {
        cmbContactos.addItem(contacto);
    }
    void addMensaje(String emisor, String mensaje) {
        txtHistorial.append("##### "+emisor + " ##### : \n" + mensaje+"\n");
    }
    void sesionIniciada(String identificador) {
        this.setTitle(" --- "+identificador+" --- ");
    }
    private String[] getIP_Puerto_Nombre() {
        String s[]=new String[3];
        s[0]=DEFAULT_IP;
        s[1]=DEFAULT_PORT;
        JTextField ip = new JTextField(20);
        JTextField puerto = new JTextField(20);
        JTextField usuario = new JTextField(20);
        ip.setText(DEFAULT_IP);
        puerto.setText(DEFAULT_PORT);
        usuario.setText("Usuario");
        JPanel myPanel = new JPanel();
        myPanel.setLayout(new GridLayout(3, 2));
        myPanel.add(new JLabel("IP del Servidor:"));
        myPanel.add(ip);
        myPanel.add(new JLabel("Puerto de la conexión:"));
        myPanel.add(puerto);
        myPanel.add(new JLabel("Escriba su nombre:"));
        myPanel.add(usuario);        
        int result = JOptionPane.showConfirmDialog(null, myPanel, 
                 "Configuraciones de la comunicación", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
                s[0]=ip.getText();
                s[1]=puerto.getText();
                s[2]=usuario.getText();
        }else{
            System.exit(0);
        }
        return s;
    }    
    void eliminarContacto(String identificador) {
        for (int i = 0; i < cmbContactos.getItemCount(); i++) {
            if(cmbContactos.getItemAt(i).toString().equals(identificador)){
                cmbContactos.removeItemAt(i);
                return;
            }
        }
    }

    private static class jPanel1 extends JPanel {

    public jPanel1() {
        setPreferredSize(new Dimension(200, 200));
        setBackground(Color.WHITE);
        JLabel etiqueta = new JLabel("Camara");
        add(etiqueta);
    }
}
}
