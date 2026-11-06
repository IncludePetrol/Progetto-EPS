package laboratorio.modello;

import java.time.LocalTime;

// una riga dello storico, chi ha fatto cosa e quando
public record Evento(LocalTime orario, TipoEvento tipo, String utente, String strumento) {

    // testo leggibile per la GUI / il log
    public String descrizione() {
        return switch (tipo) {
            case ASSEGNAZIONE -> utente + " ottiene " + strumento;
            case ATTESA -> utente + " in attesa di " + strumento;
            case RESTITUZIONE -> utente + " restituisce " + strumento;
            case ANNULLAMENTO -> utente + " annulla la richiesta di " + strumento;
        };
    }
}
