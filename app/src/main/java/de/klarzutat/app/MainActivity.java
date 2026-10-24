package de.klarzutat.app;

import android.app.Activity;
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
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(codeInput.getWindowToken(), 0);
        codeInput.clearFocus();
        showMessage("Suche Produkt …", "Produktinformationen werden von Open Food Facts geladen.");
        int current = ++requestNumber;
        new Thread(() -> {
            String message = null;
            JSONObject product = null;
            HttpURLConnection connection = null;
            try {
                String fields = "product_name,brands,ingredients_text_de,ingredients_text,allergens_tags,additives_tags,nutriscore_grade,nutrition_grades,nova_group,nutriments,quantity";
                URL url = new URL("https://world.openfoodfacts.org/api/v2/product/" + code + ".json?fields=" + fields);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "KlarZutatAndroid/1.0 (https://github.com/dor638-rgb/-KlarZutat-Android2)");
                if (connection.getResponseCode() != 200) throw new Exception("HTTP " + connection.getResponseCode());
                StringBuilder data = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        data.append(line);
                        if (data.length() > 1000000) throw new Exception("Antwort zu groß");
                    }
                }
                JSONObject answer = new JSONObject(data.toString());
                if (answer.optInt("status", 0) == 1) product = answer.optJSONObject("product");
                if (product == null) message = "Dieses Produkt ist noch nicht in der Datenbank erfasst. Prüfe den Code oder scanne ein anderes Produkt.";
            } catch (Exception e) {
                message = "Die Produktdaten konnten nicht geladen werden. Bitte prüfe deine Internetverbindung und versuche es noch einmal.";
            } finally {
                if (connection != null) connection.disconnect();
            }
            final JSONObject found = product;
            final String error = message;
            runOnUiThread(() -> {
                if (current != requestNumber) return;
                if (found != null) displayProduct(code, found);
                else showMessage("Keine Produktdaten", error);
            });
        }).start();
    }

    private void displayProduct(String code, JSONObject p) {
        resultArea.removeAllViews();
        LinearLayout summary = resultCard();
        String name = first(p.optString("product_name"), "Produkt " + code);
        summary.addView(label(name, 23, DARK, true));
        String brand = p.optString("brands");
        if (!brand.isEmpty()) summary.addView(info(brand));
        summary.addView(info("Code: " + code));
        summary.addView(info("Menge: " + first(p.optString("quantity"), "nicht angegeben")));

        String grade = first(p.optString("nutriscore_grade"), p.optString("nutrition_grades")).toUpperCase(Locale.ROOT);
        String score;
        int tint;
        if (grade.matches("[AB]")) { score = "Günstigeres Nährwertprofil"; tint = GREEN; }
        else if (grade.equals("C")) { score = "Mittleres Nährwertprofil"; tint = Color.rgb(161, 108, 26); }
        else if (grade.matches("[DE]")) { score = "Weniger günstiges Nährwertprofil"; tint = Color.rgb(165, 66, 48); }
        else { grade = "?"; score = "Keine Bewertung vorhanden"; tint = MUTED; }
        TextView verdict = label("Nutri-Score " + grade + "  ·  " + score, 17, tint, true);
        verdict.setPadding(0, dp(20), 0, dp(4));
        summary.addView(verdict);
        summary.addView(info("Der Nutri-Score bewertet das Nährwertprofil, nicht die persönliche Gesundheit."));

        LinearLayout ingredientsCard = resultCard();
        ingredientsCard.addView(label("🥬  Zutaten", 20, DARK, true));
        String ingredients = first(p.optString("ingredients_text_de"), p.optString("ingredients_text"));
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
        screen.post(() -> screen.smoothScrollTo(0, resultArea.getTop()));
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
