package pro.javax.billing.kyivstar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.List;

public class BillingCheck {
    private PopupMenu popup = new PopupMenu();

    ActionListener trayIconListener = new ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            update();
        }
    };

    public static void main(String[] args) {
        new BillingCheck();
    }

    public BillingCheck() {
        if (!SystemTray.isSupported()) {
            System.out.println("System  tray isn't supported!");
            return;
        }

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        final TrayIcon trayIcon =
                new TrayIcon(new ImageIcon(ClassLoader.getSystemResource("star.gif")).getImage());
        trayIcon.setPopupMenu(popup);
        final SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
            return;
        }

//        SystemTrayAdapter trayAdapter = new SystemTrayProvider().getSystemTray();
//        TrayIconAdapter activeTrayIcon = trayAdapter.createAndAddTrayIcon(BillingCheck.class.getResource("/star.svg"),
//                                                                          "Проверка баланса My Kyivstar",
//                                                                          popup);
//
//        activeTrayIcon.addActionListener(trayIconListener);
//        activeTrayIcon.setImageAutoSize(true);

        update();
    }

    private void update() {
        for (MenuItem item : getPopUpMenuItems(getBillingData())) {
            popup.add(item);
        }
//        System.out.println("clock");
//        trayAdapter.remove(activeTrayIcon);
//
//        activeTrayIcon = trayAdapter.createAndAddTrayIcon(BillingCheck.class.getResource("/clock.svg"),
//                                                          "Обновление",
//                                                          new PopupMenu());
//        activeTrayIcon.setImageAutoSize(true);
    }

    private Map<String, String> getBillingData() {
        try {
            InputStream configPropertiesStream = ClassLoader.getSystemResourceAsStream("config.properties");

            Properties properties = new Properties();
            properties.load(configPropertiesStream);
            System.out.println(properties);

            String postData = String.format("isSubmitted=true&USERNAME=&ORIG_URL=&isInetUser=null&buser=&bpath=&user=%s&password=%s",
                                            properties.getProperty("login"),
                                            properties.getProperty("password"));

            String postUrl = properties.getProperty("login.url");
            HttpsURLConnection postConnection = (HttpsURLConnection)new URL(postUrl).openConnection();
            postConnection.setDoOutput(true);
            postConnection.setRequestMethod("POST");
            postConnection.setRequestProperty("User-Agent",
                                              "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML," +
                                              "like Gecko) Chrome / 36.0 .1985 .125 Safari / 537.36 "
                                             );

            postConnection.getOutputStream().write(postData.getBytes());
            int responseCode = postConnection.getResponseCode();

            if (responseCode == 200) {
                Scanner scanner = new Scanner(postConnection.getInputStream(), "windows-1251");

                StringBuilder rawHTML = new StringBuilder();

                while (scanner.hasNextLine()) {
                    rawHTML.append(scanner.nextLine());
                }

                Document document = Jsoup.parse(rawHTML.toString());
                Element billInfoTable = document.select(
                        "html body#type-f table#layout.layout tbody tr#content.layout td.layout tbody tr td.layout table#mainContentTable " +
                        "tbody tr.layout td#mainContentBlock.layout table").get(0);

                Elements billInfoRows = billInfoTable.getElementsByTag("tr");

                Map<String, String> bill = new HashMap<String, String>(billInfoRows.size());

                for (Element row : billInfoRows) {
                    Elements elemParamValue = row.getElementsByTag("td");

                    if (elemParamValue.size() != 1) {
                        String param = elemParamValue.get(0).text();
                        String value = elemParamValue.get(1).text();
                        bill.put(param, value);
                    }
                }

                return bill;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private List<MenuItem> getPopUpMenuItems(Map<String, String> billData) {
        List<MenuItem> items = new ArrayList<MenuItem>(billData.size() + 1);

        for (Map.Entry<String, String> billEntry : billData.entrySet()) {
            items.add(new MenuItem(billEntry.getKey() + " " + billEntry.getValue()));
        }

        MenuItem exitItem = new MenuItem("Выход");

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                System.exit(0);
            }
        });

        items.add(exitItem);

        return items;
    }

}
