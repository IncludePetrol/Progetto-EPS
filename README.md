# Laboratorio

Progetto di Elementi di Progettazione Software, A.A. 2026/2027.

## **RUNTIME TERRORS**
### Andrea Parolini, Giacomo Valentini, Aida Selimovic

Requisiti: **JDK 17** o superiore. Le librerie necessarie sono già nella cartella `lib/`.

## Compilare ed eseguire il programma

Windows (PowerShell):

```powershell
javac -d out -cp "lib/*" (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "out;lib/*" laboratorio.Main
```

Linux / macOS:

```bash
javac -d out -cp "lib/*" $(find src -name "*.java")
java -cp "out:lib/*" laboratorio.Main
```

## Eseguire i test

Dopo aver compilato il programma:

Windows (PowerShell):

```powershell
javac -d out-test -cp "out;lib/*" (Get-ChildItem -Recurse test -Filter *.java).FullName
java -jar lib/junit-platform-console-standalone-6.0.0.jar execute -cp "out;out-test" --scan-classpath
```

Linux / macOS:

```bash
javac -d out-test -cp "out:lib/*" $(find test -name "*.java")
java -jar lib/junit-platform-console-standalone-6.0.0.jar execute -cp "out:out-test" --scan-classpath
```

## Javadoc

La Javadoc è disponibile hostata su https://includepetrol.github.io/Progetto-EPS/
