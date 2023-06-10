import Gps.Gps;
import IO.IO;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.time.*;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception
    {
        Scanner sc = new Scanner(System.in);
        Gps gps = null;
        Gps resgps = null;
        int id = 0;
        double lat = 0;
        double lon = 0;
        LocalTime t = null;
        int sat = 0;
        List<Gps> gpsList = new ArrayList<>();
        List<String> outstr;
        List<List<String>> strList = IO.CSVRead();
        Queue<Gps> gQueue;

        Gps.GPSQUEUE = new LinkedList<>(); 

        System.out.println(LocalTime.now());

        for (int i = 0; i < strList.size(); i++)
        {
            id = Integer.parseInt(strList.get(i).get(0));
            lat = Double.parseDouble(strList.get(i).get(1));
            lon = Double.parseDouble(strList.get(i).get(2));
            t = LocalTime.parse(strList.get(i).get(3));
            sat = Integer.parseInt(strList.get(i).get(4));
            gps = new Gps(id, lat, lon, t, sat);
            gpsList.add(gps);
        }
        //========================================================
        while(true)
        {
            System.out.print("ID: ");
            id = sc.nextInt();

            if(id == -1)
            {
                break;
            }

            System.out.print("LAT: ");
            lat = sc.nextDouble();
            System.out.print("LON: ");
            lon = sc.nextDouble();
            System.out.print("TIME: ");
            sc.nextLine();
            t = LocalTime.parse(sc.nextLine());
            System.out.print("SAT: ");
            sat = sc.nextInt();

            gps = new Gps(id, lat, lon, t, sat);

            if (gps.satellite < 4)
            {
                gQueue = gps.aroundGpsList(gpsList, 9);

                if (gQueue != null)
                {
                    resgps=gps.avgLocation(gQueue);
                }

                else
                {
                    gQueue = new LinkedList<>();
                    gQueue.add(gps.gpsByv());
                    resgps = gps.avgLocation(gQueue);
                }
            }

            else if (gps.satellite <= 5)
            {
                gQueue = gps.aroundGpsList(gpsList, 3);

                if (gQueue != null)
                {
                    resgps = gps.avgLocation(gQueue);
                }
                
                else
                {
                    gQueue = new LinkedList<>();
                    gQueue.add(gps.gpsByv());
                    resgps = gps.avgLocation(gQueue);
                }
            }

            else
            {
                resgps = gps;
                //파일 출력
                outstr = new ArrayList<String>();
                outstr.add(String.valueOf(id));
                outstr.add(String.valueOf(lat));
                outstr.add(String.valueOf(lon));
                outstr.add(t.toString());
                outstr.add(String.valueOf(sat));
                IO.CSVWrie(outstr);
            }
            System.out.println("\n\nLAT LON: " + resgps.nowLatitude + " " + resgps.nowLongitude+ "\n");
            resgps.addgpsQueue();
        }
        sc.close();
    }
}