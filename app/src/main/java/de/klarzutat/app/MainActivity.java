package de.klarzutat.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(20, 99, 60);
    private static final int DARK = Color.rgb(29, 48, 38);
    private static final int MUTED = Color.rgb(92, 110, 100);
    private static final int BG = Color.rgb(245, 249, 245);
    private static final int WHITE = Color.WHITE;
    private LinearLayout content;
    private EditText codeInput;
    private ScrollView screen;
    private LinearLayout resultArea;
    private int requestNumber = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(Color.rgb(245, 249, 245));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        screen = new ScrollView(this);
        screen.setFillViewport(true);
        screen.setBackgroundColor(BG);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(28), dp(20), dp(32));
        screen.addView(content);
        setContentView(screen);

        TextView title = label("🌿  KlarZutat", 30, GREEN, true);
        content.addView(title);
        TextView tagline = label("Wissen, was in deinem Essen steckt.", 16, MUTED, false);
        tagline.setPadding(0, dp(7), 0, dp(23));
        content.addView(tagline);

        LinearLayout scanCard = card();
        scanCard.addView(label("Lebensmittel prüfen", 22, DARK, true));
        TextView explanation = label("Scanne den Strichcode oder QR-Code auf der Verpackung.", 15, MUTED, false);
        explanation.setPadding(0, dp(8), 0, dp(18));
        scanCard.addView(explanation);
        Button scanButton = button("▣  Jetzt scannen", GREEN, WHITE);
        scanCard.addView(scanButton);
        scanButton.setOnClickListener(v -> startScanner());

        TextView orText = label("ODER PRODUKTCODE EINGEBEN", 12, MUTED, true);
        orText.setPadding(0, dp(20), 0, dp(10));
        scanCard.addView(orText);
        codeInput = new EditText(this);
        codeInput.setHint("z. B. 3017620422003");
        codeInput.setSingleLine(true);
        codeInput.setTextSize(17);
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        codeInput.setPadding(dp(14), dp(8), dp(14), dp(8));
        codeInput.setBackground(shape(Color.rgb(248, 250, 248), 12, Color.rgb(213, 225, 215)));
        scanCard.addView(codeInput, new LinearLayout.LayoutParams(-1, dp(54)));
        Button searchButton = button("Produkt suchen", Color.rgb(227, 240, 231), GREEN);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(50));
        searchParams.topMargin = dp(10);
        scanCard.addView(searchButton, searchParams);
        searchButton.setOnClickListener(v -> loadCode(codeInput.getText().toString()));

        resultArea = new LinearLayout(this);
        resultArea.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(-1, -2);
        resultParams.topMargin = dp(15);
        content.addView(resultArea, resultParams);

        TextView disclaimer = label("Die Angaben stammen aus Open Food Facts. Für Allergien und die tatsächlichen Zutaten ist immer die Verpackung maßgeblich.", 12, MUTED, false);
        disclaimer.setPadding(dp(8), dp(22), dp(8), 0);
        content.addView(disclaimer);

        showMessage("Bereit zum Scannen", "Richte die Kamera auf einen Lebensmittelcode oder gib seine Nummer ein.");
    }

    private void startScanner() {
        try {
            GmsBarcodeScanning.getClient(this,
                    new GmsBarcodeScannerOptions.Builder().enableAutoZoom().build())
                .startScan()
                .addOnSuccessListener(barcode -> {
                    String raw = barcode.getRawValue();
                    String code = extractCode(raw == null ? "" : raw);
                    if (code == null) {
                        showMessage("QR-Code nicht erkannt", "Dieser QR-Code enthält keinen unterstützten Lebensmittel-Produktcode.");
                    } else {
                        codeInput.setText(code);
                        loadCode(code);
                    }
                })
                .addOnFailureListener(e ->
                    showMessage("Scanner nicht verfügbar", "Prüfe deine Internetverbindung und aktualisiere die Google Play-Dienste. Alternativ kannst du den Code eintippen."));
        } catch (Exception e) {
            showMessage("Scanner nicht verfügbar", "Bitte gib den Produktcode vorerst von Hand ein.");
        }
    }

    private String extractCode(String input) {
        String raw = input == null ? "" : input.trim();
        if (raw.matches("(?:[0-9]{8}|[0-9]{12,14})")) return raw;
        Matcher off = Pattern.compile("(?i)https?://[^/]*openfoodfacts\\.org/(?:product|produit|produkt)/([0-9]{8,14})(?:[/?#]|$)").matcher(raw);
        if (off.find()) return off.group(1);
        Matcher gs1 = Pattern.compile("(?i)https?://[^/]+/01/([0-9]{14})(?:[/?#]|$)").matcher(raw);
        if (gs1.find()) return gs1.group(1);
        return null;
    }

    private boolean validGtin(String code) {
        if (!code.matches("(?:[0-9]{8}|[0-9]{12,14})")) return false;
        int sum = 0;
        boolean weight3 = true;
        for (int i = code.length() - 2; i >= 0; i--) {
            sum += (code.charAt(i) - '0') * (weight3 ? 3 : 1);
            weight3 = !weight3;
        }
        return (10 - (sum % 10)) % 10 == code.charAt(code.length() - 1) - '0';
    }

    private void loadCode(String raw) {
        String code = extractCode(raw);
        if (code == null || !validGtin(code)) {
            showMessage("Ungültiger Produktcode", "Bitte eine gültige EAN-, UPC- oder GTIN-Nummer mit Prüfziffer eingeben.");
            return;
        }
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(codeInput.getWindowToken(), 0);
        codeInput.clearFocus();
        showMessage("Suche Produkt …", "Produktinformationen werden von Open Food Facts geladen.");
        int current = ++requestNumber;

        new Thread(() -> {
            JSONObject product = null;
            String message = null;
            boolean notFound = false;

            try {
                product = fetchProduct("world.openfoodfacts.org", code);
                notFound = product == null;
            } catch (Exception first) {
                Exception actual = first;
                // Nur bei Verbindungsproblemen / Serverfehlern einen zweiten Host versuchen.
                // Bei Rate-Limits und Zugriffssperren vermeiden wir unnötige Anfragen.
                if (canTryFallback(first)) {
                    try {
                        product = fetchProduct("de.openfoodfacts.org", code);
                        notFound = product == null;
                        actual = null;
                    } catch (Exception second) {
                        actual = second;
                    }
                }
                if (actual != null) message = explainNetworkError(actual);
            }

            if (notFound) {
                message = "Für diesen gültigen Strichcode sind bei Open Food Facts derzeit keine Produktdaten hinterlegt. Das bedeutet nicht, dass das Produkt unbekannt oder ungesund ist.";
            }

            final JSONObject found = product;
            final String info = message;
            final boolean missing = notFound;
            runOnUiThread(() -> {
                if (current != requestNumber) return;
                if (found != null) {
                    displayProduct(code, found);
                } else {
                    showLookupIssue(missing ? "Produkt nicht erfasst" : "Verbindung fehlgeschlagen",
                            info == null ? "Unerwartete Antwort von Open Food Facts." : info, code);
                }
            });
        }).start();
    }

    private JSONObject fetchProduct(String host, String code) throws Exception {
        HttpURLConnection connection = null;
        try {
            String fields = "product_name_de,product_name,brands,ingredients_text_de,ingredients_text,allergens_tags,additives_tags,nutriscore_grade,nutrition_grades,nova_group,nutriments,nutrient_levels,quantity";
            URL url = new URL("https://" + host + "/api/v2/product/" + code + ".json?lc=de&fields=" + fields);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Language", "de,en;q=0.8");
            connection.setRequestProperty("User-Agent", "KlarZutatAndroid/1.1 (https://github.com/dor638-rgb/-KlarZutat-Android2)");
            int status = connection.getResponseCode();
            // Open Food Facts liefert für einen nicht gefundenen Produktcode teils HTTP 404.
            // Das ist kein Verbindungsfehler und soll verständlich erklärt werden.
            if (status == 404) return null;
            if (status != 200) throw new java.io.IOException("HTTP " + status);
            StringBuilder data = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    data.append(line);
                    if (data.length() > 1000000) throw new java.io.IOException("Antwort zu groß");
                }
            }
            JSONObject answer = new JSONObject(data.toString());
            return answer.optInt("status", 0) == 1 ? answer.optJSONObject("product") : null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private boolean canTryFallback(Exception problem) {
        String msg = problem.getMessage() == null ? "" : problem.getMessage();
        if (msg.contains("HTTP 403") || msg.contains("HTTP 429") || msg.contains("HTTP 400"))
            return false;
        return problem instanceof java.io.IOException;
    }

    private String explainNetworkError(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("HTTP 403"))
            return "Open Food Facts hat die Anfrage abgelehnt (HTTP 403). Öffne das Produkt über den Button unten im Browser.";
        if (message.contains("HTTP 429"))
            return "Zu viele Anfragen (HTTP 429). Bitte versuche es später erneut.";
        if (message.contains("HTTP 503") || message.contains("HTTP 502") || message.contains("HTTP 500"))
            return "Der Produktdaten-Dienst ist momentan nicht erreichbar (" + message + "). Versuche es später erneut.";
        if (e instanceof java.net.UnknownHostException)
            return "Open Food Facts konnte nicht erreicht werden (DNS). Prüfe WLAN, mobile Daten oder den privaten DNS-Dienst auf deinem Handy.";
        if (e instanceof java.net.SocketTimeoutException)
            return "Die Anfrage hat zu lange gedauert (Zeitüberschreitung). Bitte versuche es erneut.";
        if (e instanceof javax.net.ssl.SSLException)
            return "Die sichere HTTPS-Verbindung ist fehlgeschlagen. Prüfe Datum/Uhrzeit und Android-Systemupdates.";
        if (message.startsWith("HTTP "))
            return "Open Food Facts antwortet mit " + message + ". Teste den Browser-Link unten.";
        return "Verbindungs- oder Datenfehler (" + e.getClass().getSimpleName() + "). Teste den Browser-Link unten und prüfe deine Internetverbindung.";
    }

    private void showLookupIssue(String title, String message, String code) {
        showMessage(title, message);
        if (resultArea.getChildCount() == 0) return;
        LinearLayout card = (LinearLayout) resultArea.getChildAt(0);
        Button browser = button("Produkt im Browser öffnen", Color.rgb(227, 240, 231), GREEN);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50));
        params.topMargin = dp(14);
        card.addView(browser, params);
        browser.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://de.openfoodfacts.org/product/" + code));
            try {
                startActivity(intent);
            } catch (Exception ignored) {
                showMessage("Browser nicht verfügbar", "Öffne https://world.openfoodfacts.org im Browser.");
            }
        });
    }

    private void displayProduct(String code, JSONObject p) {
        resultArea.removeAllViews();
        LinearLayout summary = resultCard();
        String name = first(p.optString("product_name_de"), first(p.optString("product_name"), "Produkt " + code));
        summary.addView(label(name, 23, DARK, true));
        String brand = p.optString("brands");
        if (!brand.isEmpty()) summary.addView(info(brand));
        summary.addView(info("Code: " + code));
        summary.addView(info("Menge: " + first(p.optString("quantity"), "nicht angegeben")));

        addTrafficLight(summary, p);

        LinearLayout ingredientsCard = resultCard();
        ingredientsCard.addView(label("🥬  Zutaten", 20, DARK, true));
        String ingredients = first(p.optString("ingredients_text_de"), p.optString("ingredients_text"));
        if (p.optString("ingredients_text_de").trim().isEmpty() && !ingredients.isEmpty()) {
            ingredientsCard.addView(info("Hinweis: Eine deutsche Zutatenliste ist für dieses Produkt noch nicht vorhanden. Originaltext:"));
        }
        ingredientsCard.addView(paragraph(ingredients.isEmpty() ? "Keine Zutatenliste in der Datenbank vorhanden." : ingredients));

        LinearLayout allergyCard = resultCard();
        allergyCard.addView(label("⚠  Allergene", 20, DARK, true));
        allergyCard.addView(paragraph(tagList(p.optJSONArray("allergens_tags"), true)));
        allergyCard.addView(info("Wichtig bei Allergien: Angaben auf der Packung immer selbst kontrollieren."));

        LinearLayout nutritionCard = resultCard();
        nutritionCard.addView(label("📊  Nährwerte", 20, DARK, true));
        nutritionCard.addView(info("Pro 100 g bzw. 100 ml (je nach Produkt)"));
        JSONObject n = p.optJSONObject("nutriments");
        if (n == null) {
            nutritionCard.addView(paragraph("Keine Nährwerte vorhanden."));
        } else {
            nutritionLine(nutritionCard, n, "energy-kcal_100g", "Energie", "kcal");
            nutritionLine(nutritionCard, n, "sugars_100g", "Zucker", "g");
            nutritionLine(nutritionCard, n, "fat_100g", "Fett", "g");
            nutritionLine(nutritionCard, n, "saturated-fat_100g", "Gesättigte Fettsäuren", "g");
            nutritionLine(nutritionCard, n, "salt_100g", "Salz", "g");
            nutritionLine(nutritionCard, n, "fiber_100g", "Ballaststoffe", "g");
            nutritionLine(nutritionCard, n, "proteins_100g", "Eiweiß", "g");
        }

        LinearLayout more = resultCard();
        more.addView(label("Weitere Informationen", 20, DARK, true));
        int nova = p.optInt("nova_group", 0);
        more.addView(paragraph(nova >= 1 && nova <= 4 ? "NOVA-Verarbeitungsgruppe: " + nova + " von 4" : "NOVA-Gruppe: keine Angabe"));
        more.addView(info("NOVA beschreibt den Verarbeitungsgrad und ist keine Gesundheitsdiagnose."));
        more.addView(label("Zusatzstoffe", 16, DARK, true));
        more.addView(paragraph(tagList(p.optJSONArray("additives_tags"), false)));
        more.addView(info("Ein Zusatzstoff ist nicht automatisch schädlich. Die Bewertung hängt vom Stoff, seiner Menge und dem gesamten Lebensmittel ab."));
        screen.post(() -> screen.smoothScrollTo(0, resultArea.getTop()));
    }

    /**
     * Die Ampel steht ausschließlich für das Nährwertprofil nach Nutri-Score.
     * Einzelne Zusatzstoffe oder Allergene sind keine automatische Rot-Bewertung.
     */
    private void addTrafficLight(LinearLayout summary, JSONObject product) {
        String grade = first(product.optString("nutriscore_grade"),
                             product.optString("nutrition_grades")).trim().toUpperCase(Locale.ROOT);
        int tint;
        String colorName;
        String verdict;
        String reason;
        if (grade.equals("A") || grade.equals("B")) {
            tint = Color.rgb(20, 99, 60);
            colorName = "GRÜN";
            verdict = "Günstigeres Nährwertprofil";
            reason = "Nutri-Score " + grade + ": Im Nutri-Score-Vergleich schneidet dieses Lebensmittel günstiger ab.";
        } else if (grade.equals("C")) {
            tint = Color.rgb(145, 94, 19);
            colorName = "GELB";
            verdict = "Mittleres Nährwertprofil";
            reason = "Nutri-Score C: Das Nährwertprofil liegt im mittleren Bereich.";
        } else if (grade.equals("D") || grade.equals("E")) {
            tint = Color.rgb(158, 48, 40);
            colorName = "ROT";
            verdict = "Weniger günstiges Nährwertprofil";
            reason = "Nutri-Score " + grade + ": Im Nutri-Score-Vergleich schneidet dieses Lebensmittel weniger günstig ab.";
        } else {
            tint = Color.rgb(95, 106, 100);
            colorName = "GRAU";
            verdict = "Noch keine Ampel-Bewertung möglich";
            reason = "Für dieses Produkt ist kein verlässlicher Nutri-Score hinterlegt. Wir geben deshalb keine Gesundheitsfarbe vor.";
        }

        LinearLayout traffic = new LinearLayout(this);
        traffic.setOrientation(LinearLayout.VERTICAL);
        traffic.setPadding(dp(16), dp(15), dp(16), dp(15));
        traffic.setBackground(shape(tint, 15, tint));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(17);
        summary.addView(traffic, params);

        traffic.addView(label("●  " + colorName + "  ·  " + verdict, 18, WHITE, true));
        TextView explanation = label(reason, 14, WHITE, false);
        explanation.setPadding(0, dp(9), 0, 0);
        traffic.addView(explanation);

        JSONObject levels = product.optJSONObject("nutrient_levels");
        StringBuilder details = new StringBuilder();
        appendNutrientLevel(details, levels, "sugars", "Zucker");
        appendNutrientLevel(details, levels, "salt", "Salz");
        appendNutrientLevel(details, levels, "saturated-fat", "Gesättigte Fettsäuren");
        appendNutrientLevel(details, levels, "fat", "Fett");
        if (details.length() > 0) {
            TextView nutrientHints = label("Einzelwerte laut Datenbank: " + details, 13, WHITE, false);
            nutrientHints.setPadding(0, dp(11), 0, 0);
            traffic.addView(nutrientHints);
        }

        TextView limit = info("Die Ampel beurteilt nur das Nährwertprofil. Sie sagt nicht allein, ob ein Lebensmittel für dich gesund oder ungesund ist.");
        limit.setPadding(0, dp(10), 0, 0);
        summary.addView(limit);

        JSONArray allergens = product.optJSONArray("allergens_tags");
        if (allergens != null && allergens.length() > 0) {
            TextView allergyAlert = label("⚠ Allergene erfasst: " + tagList(allergens, true), 14,
                                        Color.rgb(145, 50, 36), true);
            allergyAlert.setPadding(0, dp(12), 0, 0);
            summary.addView(allergyAlert);
        }
    }

    private void appendNutrientLevel(StringBuilder b, JSONObject levels, String key, String label) {
        if (levels == null) return;
        String level = levels.optString(key, "").trim().toLowerCase(Locale.ROOT);
        String german;
        switch (level) {
            case "low": german = "niedrig"; break;
            case "moderate": german = "mittel"; break;
            case "high": german = "hoch"; break;
            default: return;
        }
        if (b.length() > 0) b.append(" · ");
        b.append(label).append(": ").append(german);
    }

    private void nutritionLine(LinearLayout parent, JSONObject n, String key, String name, String unit) {
        Object value = n.opt(key);
        if (!(value instanceof Number)) return;
        double num = ((Number) value).doubleValue();
        if (Double.isNaN(num) || Double.isInfinite(num)) return;
        String formatted = String.format(Locale.GERMANY, num >= 10 ? "%.0f" : "%.1f", num);
        TextView line = label(name + ":  " + formatted + " " + unit, 15, DARK, false);
        line.setPadding(0, dp(9), 0, dp(6));
        parent.addView(line);
    }

    private String tagList(JSONArray tags, boolean allergies) {
        if (tags == null || tags.length() == 0)
            return allergies ? "Keine Allergene erfasst – dies bedeutet nicht allergenfrei." : "Keine Zusatzstoffe in der Datenbank erfasst.";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(tags.length(), 20); i++) {
            String raw = tags.optString(i).replaceFirst("^[a-z]{2}:", "").replace('-', ' ');
            switch (raw) {
                case "milk": raw = "Milch"; break;
                case "gluten": raw = "Gluten"; break;
                case "eggs": raw = "Eier"; break;
                case "nuts": raw = "Schalenfrüchte"; break;
                case "peanuts": raw = "Erdnüsse"; break;
                case "soybeans": raw = "Soja"; break;
                case "fish": raw = "Fisch"; break;
                case "celery": raw = "Sellerie"; break;
                case "mustard": raw = "Senf"; break;
                case "sesame seeds": raw = "Sesam"; break;
                case "crustaceans": raw = "Krebstiere"; break;
                case "sulphur dioxide and sulphites": raw = "Sulfite"; break;
                default: break;
            }
            if (b.length() > 0) b.append(", ");
            b.append(raw);
        }
        if (tags.length() > 20) b.append(" …");
        return b.toString();
    }

    private void showMessage(String title, String message) {
        resultArea.removeAllViews();
        LinearLayout card = resultCard();
        card.addView(label(title, 19, DARK, true));
        card.addView(paragraph(message));
    }

    private LinearLayout resultCard() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(20), dp(19), dp(20), dp(20));
        v.setBackground(shape(WHITE, 18, Color.rgb(231, 237, 232)));
        v.setElevation(dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(14);
        resultArea.addView(v, lp);
        return v;
    }

    private LinearLayout card() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(20), dp(20), dp(20), dp(20));
        v.setBackground(shape(WHITE, 18, Color.rgb(231, 237, 232)));
        v.setElevation(dp(2));
        content.addView(v, new LinearLayout.LayoutParams(-1, -2));
        return v;
    }

    private GradientDrawable shape(int fill, int corners, int outline) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(corners));
        bg.setStroke(dp(1), outline);
        return bg;
    }

    private Button button(String title, int background, int foreground) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(foreground);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(shape(background, 12, background));
        b.setMinHeight(dp(54));
        return b;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setLineSpacing(dp(2), 1.0f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView info(String s) {
        TextView t = label(s, 13, MUTED, false);
        t.setPadding(0, dp(6), 0, 0);
        return t;
    }

    private TextView paragraph(String s) {
        TextView t = label(s, 15, DARK, false);
        t.setPadding(0, dp(10), 0, dp(6));
        return t;
    }

    private String first(String candidate, String fallback) {
        return candidate == null || candidate.trim().isEmpty() || candidate.equals("null") ? fallback : candidate;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
