package com.nefentus.api.Services;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CSVDataLoader {
    private List<String> csvData = new ArrayList<>();

    public void loadCSVData() {
        // Load the CSV data when the application starts
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("20231003-FULL-1_1.csv");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                csvData.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception appropriately
        }
    }

    public List<String> getCSVData() {
        if(csvData.isEmpty()){
            loadCSVData();
        }
        return csvData;
    }
}
