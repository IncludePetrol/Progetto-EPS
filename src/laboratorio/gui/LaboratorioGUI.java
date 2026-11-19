package laboratorio.gui;

import laboratorio.core.Laboratorio;
import laboratorio.eventi.DispatcherEventi;
import laboratorio.modello.*;
import laboratorio.persistenza.Archivio;
import laboratorio.simulazione.Simulazione;
import mdlaf.MaterialLookAndFeel;
import mdlaf.themes.MaterialLiteTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Interfaccia grafica del laboratorio.
 * <ul>
 *   <li>a sinistra: configurazione e avvio della simulazione, sotto l'elenco degli utenti;</li>
 *   <li>al centro: un tab per ogni strumento con stato e coda di attesa del suo tipo;</li>
 *   <li>a destra: registro eventi, con salvataggio e caricamento di catalogo e storico.</li>
 * </ul>
 * La GUI non tiene mai il monitor: legge lo stato con {@code snapshot()} (copia immutabile)
 * e riceve gli eventi dal {@link DispatcherEventi}, che gira su un thread separato.
 */
public class LaboratorioGUI extends JFrame {

    private static final TipoStrumento[] TIPI = TipoStrumento.values();

    private final Laboratorio lab = new Laboratorio();
    private final Archivio archivio = new Archivio(Path.of("dati"));
    private Simulazione sim;
    // richieste già presenti all'avvio della simulazione corrente: la tabella utenti mostra solo quelle successive
    private int primaRichiesta = 0;

    // configurazione
    private final JSpinner[] quantiPerTipo = new JSpinner[TIPI.length];
    private final JSpinner utenti = new JSpinner(new SpinnerNumberModel(6, Simulazione.MIN_UTENTI, Simulazione.MAX_UTENTI, 1));
    private final JSpinner giri = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
    private final JSpinner durata = new JSpinner(new SpinnerNumberModel(800, 0, 10_000, 100));
    private final JButton avvia = new JButton("Avvia simulazione");
    private final JButton interrompi = new JButton("Interrompi");
    private final JLabel stato = new JLabel();

    // utenti
    private final DefaultTableModel modelloUtenti = new DefaultTableModel(new Object[]{"Utente", "Strumento", "Stato"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    // strumenti: griglia 2x2 (max 4)
    private final JPanel grigliaStrumenti = new JPanel(new GridLayout(2, 2, 8, 8));
    private List<String> codiciGriglia = null;
    private final Map<String, JLabel> infoStrumenti = new HashMap<>();
    private final Map<String, DefaultListModel<String>> codeStrumenti = new HashMap<>();

    // colore assegnato a ogni utente
    private final Map<String, Color> coloriUtenti = new HashMap<>();

    // registro eventi
    private final DefaultListModel<String> modelloLog = new DefaultListModel<>();
    private final JList<String> log = new JList<>(modelloLog);
    private final JButton salva = new JButton("Salva dati");
    private final JButton carica = new JButton("Carica dati");

    public LaboratorioGUI() {
        super("Laboratorio · Prestito strumenti");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(pannelloSinistro(), BorderLayout.WEST);
        add(grigliaStrumenti, BorderLayout.CENTER);
        add(pannelloLog(), BorderLayout.EAST);

        caricaDati(false); // requisito all'avvio si ricaricano catalogo e storico
        if (lab.snapshot().strumenti().isEmpty()) {
            quantiPerTipo[0].setValue(2); // catalogo di default
            quantiPerTipo[1].setValue(1);
            quantiPerTipo[2].setValue(1);
            applicaCatalogo();
        }

        // il dispatcher consegna gli eventi su un suo thread
        DispatcherEventi dispatcher = new DispatcherEventi(lab.getRegistro());
        dispatcher.aggiungi(e -> SwingUtilities.invokeLater(() -> {
            aggiungiLog(e);
            aggiorna(); // ogni evento aggiorna subito griglia e utenti
        }));
        Thread t = new Thread(dispatcher, "dispatcher-eventi");
        t.setDaemon(true);
        t.start();

        // aggiornamento periodico dello stato su copia di snapshot
        new javax.swing.Timer(200, e -> aggiorna()).start();
        aggiorna();

        setSize(1150, 650);
        setLocationRelativeTo(null);
    }

    // costruzione pannelli 

    private JPanel pannelloSinistro() {
        JPanel config = new JPanel(new GridBagLayout());
        config.setBorder(BorderFactory.createTitledBorder("Simulazione"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        int riga = 0;
        for (int i = 0; i < TIPI.length; i++) {
            quantiPerTipo[i] = new JSpinner(new SpinnerNumberModel(0, 0, Laboratorio.MAX_STRUMENTI, 1));
            riga = rigaForm(config, c, riga, nomeTipo(TIPI[i]), quantiPerTipo[i]);
        }
        riga = rigaForm(config, c, riga, "Utenti (thread)", utenti);
        riga = rigaForm(config, c, riga, "Giri per utente", giri);
        riga = rigaForm(config, c, riga, "Durata prestito (ms)", durata);

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy = riga++;
        config.add(avvia, c);
        c.gridy = riga++;
        config.add(interrompi, c);
        c.gridy = riga;
        config.add(stato, c);

        avvia.addActionListener(e -> avviaSimulazione());
        interrompi.addActionListener(e -> sim.interrompi());

        JTable tabella = new JTable(modelloUtenti);
        tabella.setFillsViewportHeight(true);
        JScrollPane scrollUtenti = new JScrollPane(tabella);
        scrollUtenti.setBorder(BorderFactory.createTitledBorder("Utenti"));

        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setPreferredSize(new Dimension(300, 0));
        p.add(config, BorderLayout.NORTH);
        p.add(scrollUtenti, BorderLayout.CENTER);
        return p;
    }

    private static int rigaForm(JPanel p, GridBagConstraints c, int riga, String etichetta, JComponent campo) {
        c.gridwidth = 1;
        c.gridy = riga;
        c.gridx = 0;
        c.weightx = 1;
        p.add(new JLabel(etichetta), c);
        c.gridx = 1;
        c.weightx = 0;
        p.add(campo, c);
        return riga + 1;
    }

    private JPanel pannelloLog() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setPreferredSize(new Dimension(320, 0));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createTitledBorder("Registro eventi"));
        p.add(scroll, BorderLayout.CENTER);

        JPanel bottoni = new JPanel(new GridLayout(1, 2, 8, 0));
        bottoni.add(salva);
        bottoni.add(carica);
        p.add(bottoni, BorderLayout.SOUTH);

        salva.addActionListener(e -> salvaDati());
        carica.addActionListener(e -> caricaDati(true));
        return p;
    }

    // azioni 

    private void avviaSimulazione() {
        try {
            applicaCatalogo();
            Simulazione nuova = new Simulazione(lab, (int) utenti.getValue(), (int) giri.getValue(), (int) durata.getValue());
            primaRichiesta = lab.snapshot().richieste().size();
            sim = nuova;
            sim.avvia();
        } catch (RuntimeException ex) {
            errore(ex.getMessage());
        }
        aggiorna();
    }

    // se le quantità scelte sono diverse dal catalogo attuale, lo ricrea
    private void applicaCatalogo() {
        int totale = 0;
        for (JSpinner s : quantiPerTipo) {
            totale += (int) s.getValue();
        }
        if (totale < 1 || totale > Laboratorio.MAX_STRUMENTI) {
            throw new IllegalArgumentException("servono da 1 a " + Laboratorio.MAX_STRUMENTI + " strumenti in tutto");
        }
        if (Arrays.equals(contaPerTipo(lab.snapshot()), quantiScelti())) {
            return;
        }
        List<Strumento> catalogo = new ArrayList<>();
        for (int i = 0; i < TIPI.length; i++) {
            for (int n = 1; n <= (int) quantiPerTipo[i].getValue(); n++) {
                catalogo.add(new Strumento(String.format("%s-%02d", TIPI[i].name().substring(0, 3), n), TIPI[i]));
            }
        }
        lab.ripristina(catalogo);
    }

    private void salvaDati() {
        try {
            archivio.salva(lab);
            JOptionPane.showMessageDialog(this, "Catalogo e storico salvati nella cartella \"dati\".");
        } catch (IOException ex) {
            errore("Salvataggio fallito: " + ex.getMessage());
        }
    }

    private void caricaDati(boolean avvisa) {
        try {
            archivio.carica(lab);
        } catch (IOException | RuntimeException ex) {
            if (avvisa) {
                errore("Caricamento fallito: " + ex.getMessage());
            }
            return;
        }
        int[] conta = contaPerTipo(lab.snapshot());
        for (int i = 0; i < TIPI.length; i++) {
            quantiPerTipo[i].setValue(conta[i]);
        }
        modelloLog.clear();
        lab.getRegistro().getStorico().forEach(this::aggiungiLog);
        aggiorna();
    }

    // aggiornamento della vista

    private void aggiungiLog(Evento e) {
        String resto = e.descrizione().substring(e.utente().length());
        modelloLog.addElement("<html>" + e.orario().withNano(0) + "&nbsp;&nbsp;" + utente(e.utente()) + resto + "</html>");
        log.ensureIndexIsVisible(modelloLog.size() - 1);
    }

    private void aggiorna() {
        StatoLab s = lab.snapshot();
        boolean attiva = sim != null && sim.isAttiva();

        avvia.setEnabled(!attiva);
        interrompi.setEnabled(attiva);
        carica.setEnabled(!attiva);
        for (JSpinner sp : quantiPerTipo) {
            sp.setEnabled(!attiva);
        }
        utenti.setEnabled(!attiva);
        giri.setEnabled(!attiva);
        durata.setEnabled(!attiva);
        stato.setText("<html>Disponibili: <b>" + s.disponibili() + "</b> · In prestito: <b>" + s.inPrestito()
                + "</b> · In attesa: <b>" + s.inAttesa() + "</b><br>Thread attivi: <b>"
                + (sim == null ? 0 : sim.threadAttivi()) + "</b></html>");

        aggiornaUtenti(s);
        aggiornaStrumenti(s);
    }

    // per ogni utente della simulazione corrente mostra la richiesta più recente
    private void aggiornaUtenti(StatoLab s) {
        Map<String, RichiestaView> ultima = new LinkedHashMap<>();
        List<RichiestaView> richieste = s.richieste();
        for (RichiestaView r : richieste.subList(Math.min(primaRichiesta, richieste.size()), richieste.size())) {
            ultima.put(r.utente(), r);
        }
        modelloUtenti.setRowCount(0);
        for (RichiestaView r : ultima.values()) {
            String strumento = switch (r.stato()) {
                case IN_PRESTITO -> r.codiceStrumento();
                case IN_ATTESA -> nomeTipo(r.tipo());
                default -> "-";
            };
            modelloUtenti.addRow(new Object[]{"<html>" + utente(r.utente()) + "</html>", strumento, testo(r.stato())});
        }
    }

    private void aggiornaStrumenti(StatoLab s) {
        // se il catalogo cambiato ricrea tab
        List<String> codici = s.strumenti().stream().map(StrumentoView::codice).toList();
        if (!codici.equals(codiciGriglia)) {
            codiciGriglia = codici;
            grigliaStrumenti.removeAll();
            infoStrumenti.clear();
            codeStrumenti.clear();
            for (String codice : codici) {
                JLabel info = new JLabel();
                DefaultListModel<String> coda = new DefaultListModel<>();
                JScrollPane scrollCoda = new JScrollPane(new JList<>(coda));
                scrollCoda.setBorder(BorderFactory.createTitledBorder("Coda di attesa (FIFO)"));
                JPanel scheda = new JPanel(new BorderLayout(0, 4));
                scheda.setBorder(BorderFactory.createTitledBorder(codice));
                scheda.add(info, BorderLayout.NORTH);
                scheda.add(scrollCoda, BorderLayout.CENTER);
                grigliaStrumenti.add(scheda);
                infoStrumenti.put(codice, info);
                codeStrumenti.put(codice, coda);
            }
            for (int i = codici.size(); i < Laboratorio.MAX_STRUMENTI; i++) {
                grigliaStrumenti.add(new JPanel()); // caselle vuote la griglia resta 2x2
            }
            grigliaStrumenti.revalidate();
            grigliaStrumenti.repaint();
        }

        for (StrumentoView sv : s.strumenti()) {
            String colore = sv.stato() == StatoStrumento.DISPONIBILE ? "#2e7d32" : "#c62828";
            infoStrumenti.get(sv.codice()).setText("<html><table>"
                    + "<tr><td>Tipo:</td><td>" + nomeTipo(sv.tipo()) + "</td></tr>"
                    + "<tr><td>Stato:</td><td><b><font color='" + colore + "'>" + testo(sv.stato()) + "</font></b></td></tr>"
                    + "<tr><td>In prestito a:</td><td>" + (sv.assegnatoA() == null ? "-" : utente(sv.assegnatoA())) + "</td></tr>"
                    + "</table></html>");

            // la coda è per tipo, chi aspetta prenderà il primo strumento libero di quel tipo
            DefaultListModel<String> coda = codeStrumenti.get(sv.codice());
            List<String> inAttesa = new ArrayList<>();
            for (RichiestaView r : s.richieste()) {
                if (r.tipo() == sv.tipo() && r.stato() == StatoRichiesta.IN_ATTESA) {
                    inAttesa.add("<html>" + (inAttesa.size() + 1) + ". " + utente(r.utente()) + "</html>");
                }
            }
            if (!Collections.list(coda.elements()).equals(inAttesa)) {
                coda.clear();
                coda.addAll(inAttesa);
            }
        }
    }

    // supporto

    // ogni utente riceve un colore la prima volta che compare, distanziato da rapporto auereo
    private String utente(String nome) {
        Color c = coloriUtenti.computeIfAbsent(nome,
                n -> Color.getHSBColor((coloriUtenti.size() * 0.618f) % 1f, 0.8f, 0.75f));
        return String.format("<b><font color='#%06x'>%s</font></b>", c.getRGB() & 0xFFFFFF, nome);
    }

    private static int[] contaPerTipo(StatoLab s) {
        int[] conta = new int[TIPI.length];
        for (StrumentoView sv : s.strumenti()) {
            conta[sv.tipo().ordinal()]++;
        }
        return conta;
    }

    private int[] quantiScelti() {
        int[] q = new int[TIPI.length];
        for (int i = 0; i < TIPI.length; i++) {
            q[i] = (int) quantiPerTipo[i].getValue();
        }
        return q;
    }

    private static String nomeTipo(TipoStrumento t) {
        String n = t.name().toLowerCase();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static String testo(Enum<?> stato) {
        String n = stato.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private void errore(String messaggio) {
        JOptionPane.showMessageDialog(this, messaggio, "Errore", JOptionPane.ERROR_MESSAGE);
    }

    // Imposta il tema e apre la finestra sull'Event Dispatch Thread.
    public static void avvia() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new MaterialLookAndFeel(new MaterialLiteTheme()));
            } catch (Exception | LinkageError e) { // LinkageError: manca un jar del tema in lib/
                System.err.println("tema Material non disponibile, uso quello di default: " + e);
            }
            new LaboratorioGUI().setVisible(true);
        });
    }
}
