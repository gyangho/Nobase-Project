package IO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IO 
{
    public static List<List<String>> CSVRead()
    {
        //반환용 리스트
        List<List<String>> ret = new ArrayList<List<String>>();
        BufferedReader br = null;
        
        try
        {
            br = Files.newBufferedReader(Paths.get("C:\\Users\\dydtk\\OneDrive\\2023\\소프트웨어프로젝트\\project\\Gps.csv"));
            //Charset.forName("UTF-8");
            String line = "";
            
            while((line = br.readLine()) != null)
            {
                //CSV 1행을 저장하는 리스트
                List<String> tmpList = new ArrayList<String>();
                String array[] = line.split(",");
                //배열에서 리스트 반환
                tmpList = Arrays.asList(array);
                System.out.println(tmpList);
                ret.add(tmpList);
            }
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if(br != null)
                {
                    br.close();
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static int CSVWrie(List<String> input)
    {
         //출력 스트림 생성
         BufferedWriter bufWriter = null;
         try
         {
            //이어쓰기
             bufWriter = Files.newBufferedWriter(Paths.get("C:\\Users\\dydtk\\OneDrive\\2023\\소프트웨어프로젝트\\project\\Gps.csv"),Charset.forName("UTF-8"), StandardOpenOption.APPEND);

            for(String data : input)
            {
                bufWriter.write(data);
                bufWriter.write(",");
            }
            //개행코드추가
            bufWriter.newLine();
         }
         catch(FileNotFoundException e)
         {
             e.printStackTrace();
         }
         catch(IOException e)
         {
             e.printStackTrace();
         }
         finally
         {
             try
             {
                 if(bufWriter != null)
                 {
                     bufWriter.close();
                 }
             }
             catch(IOException e)
             {
                 e.printStackTrace();
             }
         }
             return 0;
    }
}
