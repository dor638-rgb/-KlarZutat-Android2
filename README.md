# KlarZutat – Android-App

**KlarZutat** scannt EAN-/UPC-Strichcodes und QR-Codes und zeigt die deklarierten
Zutaten, Allergene, Zusatzstoffe, Nährwerte sowie Nutri-Score und NOVA-Gruppe
aus [Open Food Facts](https://world.openfoodfacts.org/) an.

## APK direkt auf dem Android-Handy herunterladen

1. Öffne im Browser die Seite **Actions**:
   https://github.com/dor638-rgb/-KlarZutat-Android2/actions
2. Wähle den neuesten erfolgreichen Lauf **KlarZutat APK** (grüner Haken).
3. Unten bei **Artifacts** auf **KlarZutat-APK** tippen.
4. Das heruntergeladene ZIP entpacken und **app-debug.apk** öffnen.
5. Falls Android nachfragt, die Installation aus dieser Quelle erlauben.

Du musst dafür **keinen Windows-PC** haben. GitHub-Login kann für das Herunterladen
von Actions-Artifacts erforderlich sein.

## Funktionsweise

- Google Code Scanner über Google Play-Dienste; ohne Kamera-Berechtigung in KlarZutat.
  Beim ersten Scan wird ggf. ein Scanner-Modul heruntergeladen.
- Code alternativ manuell eingeben.
- QR-Codes mit reiner GTIN, Open-Food-Facts-Produktlink oder GS1-Digital-Link erkennen.
- Produktdaten per HTTPS direkt bei Open Food Facts abrufen. Internet erforderlich.
- Nutri-Score und NOVA zeigen nur Aspekte der Produktqualität, **keine
  individuelle Gesundheitsdiagnose**.
- Bei Allergien immer die Verpackung selbst kontrollieren.
- Scans verlassen den Google-Code-Scanner nicht als Bild; nur der Produktcode
  wird bei der Produktsuche an Open Food Facts gesendet.
- Für öffentliche Verteilung: Lizenz- und Datenbedingungen von Open Food Facts prüfen.

## Bau lokal

Java 17, Android SDK 35 und Gradle 8.9:
`gradle :app:assembleDebug`
APK: `app/build/outputs/apk/debug/app-debug.apk`

Diese Version wird als Debug-APK für eigene Tests gebaut, nicht als
signierte Play-Store-Release.
