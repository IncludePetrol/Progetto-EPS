package laboratorio.core;

import laboratorio.modello.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LaboratorioTest {

    private static final TipoStrumento OSC = TipoStrumento.OSCILLOSCOPIO;

    private Laboratorio lab;

    // usati dai thread dei test di attesa
    private String codiceKimi;
    private boolean kimiInterrotto;

    @BeforeEach
    void setUp() {
        lab = new Laboratorio();
    }

    // ---------- catalogo ----------

    @Test
    void nonSiPossonoAggiungerePiuStrumentiDelMassimo() {
        for (int i = 1; i <= Laboratorio.MAX_STRUMENTI; i++) {
            lab.aggiungiStrumento(new Strumento("S-" + i, TipoStrumento.GENERATORE));
        }
        assertThrows(IllegalStateException.class,
                () -> lab.aggiungiStrumento(new Strumento("S-5", TipoStrumento.GENERATORE)));
    }

    @Test
    void nonSiPossonoAggiungereDueStrumentiConLoStessoCodice() {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        assertThrows(IllegalArgumentException.class,
                () -> lab.aggiungiStrumento(new Strumento("OSC-01", OSC)));
    }

    @Test
    void nonSiPuoRimuovereUnoStrumentoInPrestito() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC);

        assertThrows(IllegalStateException.class, () -> lab.rimuoviStrumento("OSC-01"));
    }

    // ---------- richiesta e restituzione ----------

    @Test
    void unoStrumentoDisponibileVieneAssegnato() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));

        String codice = lab.richiedi("Alysa", OSC);

        assertEquals("OSC-01", codice);
        StrumentoView s = lab.snapshot().strumenti().get(0);
        assertEquals(StatoStrumento.IN_PRESTITO, s.stato());
        assertEquals("Alysa", s.assegnatoA());
    }

    @Test
    void richiedereUnTipoNonPresenteDaErrore() {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));

        assertThrows(IllegalArgumentException.class,
                () -> lab.richiedi("Alysa", TipoStrumento.GENERATORE));
    }

    @Test
    void restituireLoStrumentoLoRendeDisponibile() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC);

        lab.restituisci("Alysa", "OSC-01");

        StrumentoView s = lab.snapshot().strumenti().get(0);
        assertEquals(StatoStrumento.DISPONIBILE, s.stato());
        assertNull(s.assegnatoA());
    }

    @Test
    void restituireLoStrumentoDaUnAltroUtenteDaErrore() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC);

        assertThrows(IllegalStateException.class, () -> lab.restituisci("Kimi", "OSC-01"));
        assertEquals("Alysa", lab.snapshot().strumenti().get(0).assegnatoA());
    }

    // ---------- attesa e interruzione (con thread) ----------

    @Test
    void unaRichiestaAspettaFinoAllaRestituzione() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC); // l'unico oscilloscopio è occupato

        Thread kimi = new Thread(() -> {
            try {
                codiceKimi = lab.richiedi("Kimi", OSC);
            } catch (InterruptedException e) {
                kimiInterrotto = true;
            }
        });
        kimi.start();
        Thread.sleep(200); // lascia tempo a Kimi di mettersi in attesa

        assertTrue(kimi.isAlive(), "Kimi non deve ottenere lo strumento mentre è occupato");

        lab.restituisci("Alysa", "OSC-01");
        kimi.join(5000);

        assertEquals("OSC-01", codiceKimi);
    }

    @Test
    void unaRichiestaInterrottaMentreAspettaVieneAnnullata() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC);

        Thread kimi = new Thread(() -> {
            try {
                codiceKimi = lab.richiedi("Kimi", OSC);
            } catch (InterruptedException e) {
                kimiInterrotto = true;
            }
        });
        kimi.start();
        Thread.sleep(200);

        kimi.interrupt();
        kimi.join(5000);

        assertTrue(kimiInterrotto);
        assertNull(codiceKimi);
        // lo strumento resta ad Alysa, Kimi non lo ha ottenuto
        assertEquals("Alysa", lab.snapshot().strumenti().get(0).assegnatoA());
        assertTrue(lab.getRegistro().getStorico().stream()
                .anyMatch(e -> e.tipo() == TipoEvento.ANNULLAMENTO));
    }

    // ---------- più utenti ----------

    @Test
    void cinqueUtentiUsanoDueStrumentiSenzaBloccarsi() throws Exception {
        lab.aggiungiStrumento(new Strumento("MUL-01", TipoStrumento.MULTIMETRO));
        lab.aggiungiStrumento(new Strumento("MUL-02", TipoStrumento.MULTIMETRO));

        List<Thread> utenti = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String nome = "Utente" + i;
            Thread t = new Thread(() -> {
                try {
                    for (int giro = 0; giro < 10; giro++) {
                        String codice = lab.richiedi(nome, TipoStrumento.MULTIMETRO);
                        lab.restituisci(nome, codice);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            utenti.add(t);
            t.start();
        }
        for (Thread t : utenti) {
            t.join(10_000);
            assertFalse(t.isAlive(), "un utente è rimasto bloccato");
        }

        StatoLab stato = lab.snapshot();
        assertEquals(2, stato.disponibili());
        assertEquals(0, stato.inPrestito());
        assertEquals(0, stato.inAttesa());
    }
}
