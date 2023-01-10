package com.databend.jdbc;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestFileTransfer
{
    private static byte[] streamToByteArray(InputStream stream) throws IOException
    {

        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int line = 0;
        // read bytes from stream, and store them in buffer
        while ((line = stream.read(buffer)) != -1) {
            // Writes bytes from byte array (buffer) into output stream.
            os.write(buffer, 0, line);
        }
        stream.close();
        os.flush();
        os.close();
        return os.toByteArray();
    }

    private Connection createConnection()
            throws SQLException
    {
        String url = "jdbc:databend://localhost:8000";
        return DriverManager.getConnection(url, "root", "root");
    }

    // generate a csv file in a temp directory with given lines, return absolute path of the generated csv
    private String generateRandomCSV(int lines) {
        if (lines <= 0) {
            return "";
        }
        String tmpDir = System.getProperty("java.io.tmpdir");
        String csvPath = tmpDir + "/test.csv";
        try {
            FileWriter writer = new FileWriter(csvPath);
            for (int i = 0; i < lines; i++) {
                int num = (int) (Math.random() * 1000);
                writer.write("a,b,c," + num + "\n");
            }
            writer.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return csvPath;
    }

    @Test(groups = {"IT"})
    public void testFileTransfer()
    {
        String filePath = generateRandomCSV(10000);
        try {
            Connection connection = createConnection();
            String stageName = "test_stage";
            DatabendConnection databendConnection = connection.unwrap(DatabendConnection.class);
            PresignContext.createStageIfNotExists(databendConnection, stageName);
            File f = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(f);
            databendConnection.uploadStream(stageName, "jdbc/test/", fileInputStream, "test.csv", false);
            InputStream inputStream = databendConnection.downloadStream(stageName, "jdbc/test/test.csv", false);
            Assert.assertNotNull(inputStream);
            byte[] got = streamToByteArray(inputStream);
            byte[] expected = streamToByteArray(new FileInputStream(f));
            Assert.assertEquals(got, expected);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}