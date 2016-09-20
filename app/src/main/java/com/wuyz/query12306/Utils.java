package com.wuyz.query12306;

import com.google.gson.Gson;
import com.wuyz.query12306.model.JsonMsg4LeftTicket;
import com.wuyz.query12306.model.TrainInfo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class Utils {
    private static final String TAG = "Utils";

    private static final String RFC = "-----BEGIN CERTIFICATE-----\n" +
            "MIICsTCCAhqgAwIBAgIIODtw6bZEH1kwDQYJKoZIhvcNAQEFBQAwRzELMAkGA1UE\n" +
            "BhMCQ04xKTAnBgNVBAoTIFNpbm9yYWlsIENlcnRpZmljYXRpb24gQXV0aG9yaXR5\n" +
            "MQ0wCwYDVQQDEwRTUkNBMB4XDTE0MDUyNjAxNDQzNloXDTE5MDUyNTAxNDQzNlow\n" +
            "azELMAkGA1UEBhMCQ04xKTAnBgNVBAoTIFNpbm9yYWlsIENlcnRpZmljYXRpb24g\n" +
            "QXV0aG9yaXR5MRkwFwYDVQQLHhCUwY3vW6JiN2cNUqFOLV/DMRYwFAYDVQQDEw1r\n" +
            "eWZ3LjEyMzA2LmNuMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC8Cxlz+V/4\n" +
            "KkUk8YTxVxzii7xp2gZPWuuVBiwQ6iwL98it75WNGiYCUasDXy3O8wY+PtZFvgEK\n" +
            "kpHqQ1U6uemiHStthUS1xTBsU/TuXF6AHc+oduP6zCGKcUnHRAksRb8BGSgzBA/X\n" +
            "3B9CUKnYa9YA2EBIYccrzIh6aRAjDHbvYQIDAQABo4GBMH8wHwYDVR0jBBgwFoAU\n" +
            "eV62d7fiUoND7cdRiExjhSwAQ1gwEQYJYIZIAYb4QgEBBAQDAgbAMAsGA1UdDwQE\n" +
            "AwIC/DAdBgNVHQ4EFgQUj/0m74jhq993ItPCldNHYLJ884MwHQYDVR0lBBYwFAYI\n" +
            "KwYBBQUHAwIGCCsGAQUFBwMBMA0GCSqGSIb3DQEBBQUAA4GBAEXeoTkvUVSeQzAx\n" +
            "FIvqfC5jvBuApczonn+Zici+50Jcu17JjqZ0zEjn4HsNHm56n8iEbmOcf13fBil0\n" +
            "aj4AQz9hGbjmvQSufaB6//LM1jVe/OSVAKB4C9NUdY5PNs7HDzdLfkQjjDehCADa\n" +
            "1DH+TP3879N5zFoWDgejQ5iFsAh0\n" +
            "-----END CERTIFICATE-----\n";

    private static Map<String, String> readStations(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader("station_name.txt"))){
            Map<String, String> stationMap = new HashMap<>(2500);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] arr = line.split("\\|");
                    if (arr.length >= 2) {
                        stationMap.put(arr[1].trim(), arr[2].trim());
                    }
                }
            }
            return stationMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //queryLeftTickets("XKS", "ARH", "2016-10-01");
    public static String queryLeftTickets(String fromStationCode, String toStationCode, String date) {
        HttpsURLConnection connection = null;
        try {
            String param = String.format("leftTicketDTO.train_date=%s" +
                    "&leftTicketDTO.from_station=%s&" +
                    "leftTicketDTO.to_station=%s&" +
                    "purpose_codes=ADULT", date, fromStationCode, toStationCode);
            URL url = new URL("https://kyfw.12306.cn/otn/leftTicket/queryT?" + param);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
//            connection.setRequestMethod("POST");
//            connection.setDoOutput(true);
            connection.setRequestProperty("Host", "kyfw.12306.cn");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:34.0) Gecko/20100101 Firefox/34.0");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            //connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("If-Modified-Since", "0");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            connection.setRequestProperty("Referer", "https://kyfw.12306.cn/");
            connection.setRequestProperty("Cookie", "tmp");
            connection.setRequestProperty("Connection", "keep-alive");

//            OutputStream outputStream = connection.getOutputStream();
//            outputStream.write(param.getBytes());

            boolean isZip = "gzip".equals(connection.getHeaderField("Content-Encoding"));
            InputStream inputStream;
            if (isZip) {
                inputStream = new GZIPInputStream(connection.getInputStream());
            } else {
                Map<String, List<String>> headers = connection.getHeaderFields();
                if (headers != null) {
                    for (String key : headers.keySet()) {
                        System.out.print(key + ":");
                        for (String v : headers.get(key)) {
                            System.out.print(v + ",");
                        }
                        System.out.println();
                    }
                }
                inputStream = connection.getInputStream();
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];
            int n;
            while ((n = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, n);
            }
            String content = byteArrayOutputStream.toString();
            System.out.println(content);
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    public static void initHttps() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        Certificate certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(RFC.getBytes()));
        keyStore.setCertificateEntry("SRCA", certificate);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        context.init(null, factory.getTrustManagers(), null);
//        context.init(null, new TrustManager[]{new EmptyTrustManager()}, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    }

    private static class EmptyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    public static List<TrainInfo> parseAvailableTrains(String content) {
        if (content == null || content.isEmpty())
            return null;
        Gson gson = new Gson();
        JsonMsg4LeftTicket ticket = gson.fromJson(content, JsonMsg4LeftTicket.class);
        if (ticket == null)
            return null;
        List<JsonMsg4LeftTicket.TrainQueryInfo> infos = ticket.getData();
        if (infos == null || infos.isEmpty())
            return null;
        List<TrainInfo> list = new ArrayList<>(4);
        for (JsonMsg4LeftTicket.TrainQueryInfo info : infos) {
            TrainInfo trainInfo = info.getQueryLeftNewDTO();
            if (trainInfo != null && "Y".equalsIgnoreCase(trainInfo.getCanWebBuy())) {
                String s = trainInfo.getZe_num();
                try {
                    if ("有".equals(s)) {
                        list.add(trainInfo);
                        continue;
                    }
                    Integer.parseInt(s);
                    list.add(trainInfo);
                    continue;
                } catch (Exception e) {
                }

                s = trainInfo.getWz_num();
                try {
                    if ("有".equals(s)) {
                        list.add(trainInfo);
                        continue;
                    }
                    Integer.parseInt(s);
                    list.add(trainInfo);
                    continue;
                } catch (Exception e) {
                }
            }
        }
        return list;
    }
}
