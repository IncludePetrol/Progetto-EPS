package laboratorio.modello;

import java.util.List;

// fotografia del laboratorio in un dato momento (usata per GUI e salvataggio)
public record StatoLab(List<StrumentoView> strumenti, List<RichiestaView> richieste) {

    public long disponibili() {
        return strumenti.stream().filter(s -> s.stato() == StatoStrumento.DISPONIBILE).count();
    }

    public long inPrestito() {
        return strumenti.stream().filter(s -> s.stato() == StatoStrumento.IN_PRESTITO).count();
    }

    public long inAttesa() {
        return richieste.stream().filter(r -> r.stato() == StatoRichiesta.IN_ATTESA).count();
    }
}
