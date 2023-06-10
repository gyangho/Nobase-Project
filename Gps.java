package Gps;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Queue;
import java.util.LinkedList;
import java.time.Duration;
import java.time.LocalTime;


public class Gps
{
    public static final int EARTH_RADIUS = 6371;//[km]
    public static final double AVG_SPEED = 2; //[m/s]
    public static Deque<Gps> GPSQUEUE;//이전 데이터와 이전전데이터 저장

    public LocalTime time;
    public int gid;
    public double nowLatitude;
    public double nowLongitude;
    public int satellite;

    public double mForLatitude;
    public double mForLongitude;
    public List<Gps> glist;

    public Gps()
    {
        time = null;
        gid = 0;
        nowLatitude = 0;
        nowLongitude = 0;
        mForLatitude = 0;
        mForLongitude = 0;
        satellite = 0;
    }

    public Gps(int i, double lat, double lon, LocalTime t, int sat)
    {
        gid = i;
        //현재 위도 좌표 (y 좌표)
        nowLatitude = lat;
        //현재 경도 좌표 (x 좌표)
        nowLongitude = lon;
        //좌표 1도 당 미터
        mForLatitude = (EARTH_RADIUS * 1000 * 1 * Math.PI / 180);
        //좌표 2도 당 미터
        mForLongitude = (EARTH_RADIUS * 1000 * 1 * (Math.PI / 180) * Math.cos(Math.toRadians(nowLatitude)));
        time = t;
        satellite = sat;

        glist = new ArrayList<Gps>();


        if (GPSQUEUE.size() == 3)
        {
            GPSQUEUE.poll();
        }

        else if (GPSQUEUE.size() > 3)
        {
            System.out.println("QUEUE.SIZE() IS TO BIG");
        }

            glist.add(0, GPSQUEUE.peek()); //이전의 이전전. 비어있으면 null들어감
            glist.add(1, GPSQUEUE.peekLast());//이전 데이터. 비어있으면 null들어감
            glist.add(2, this);//현재
    }

    public void addgpsQueue()
    {
        GPSQUEUE.add(this); //큐에 현재 gps데이터 삽입
    }

    public double available_Distance()
    {
        LocalTime t1 = glist.get(2).time; //현재시간
        LocalTime t2 = glist.get(1).time;
        Duration duration = Duration.between(t2, t1);

        return duration.toSeconds() * AVG_SPEED;
    }

    public double available_Distance(int sec)
    {
        return sec * AVG_SPEED;
    }

    public double get_distance(Gps g1, Gps g2)
    {

        double lat1 = g1.nowLatitude; 
        double lat2 = g2.nowLatitude;
        double lon1 = g1.nowLongitude;
        double lon2 = g2.nowLongitude;
        double dLat = Math.toRadians(lat2 - lat1); //위도 변화량
        double dLon = Math.toRadians(lon2 - lon1); //경도 변화량

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
         * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = EARTH_RADIUS * c * 1000;    // Haversine formula를 이용한 거리계산
        return d;
    }

    public double[] get_past_coordinate()
    {
        double[] v = new double[3];
        double lat1 = glist.get(1).nowLatitude;
        double lon1 = glist.get(1).nowLongitude;

        v[0] = Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lon1)); // x좌표
        v[1] = Math.sin(Math.toRadians(lat1)); // y좌표
        v[2] = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lon1)); // z좌표

        return v;
    }

    public double[] get_vector() //이전과 그 전의 데이터의 방향
    {
        double lat1 = glist.get(0).nowLatitude; //이이전의 위도
        double lat2 = glist.get(1).nowLatitude; //이전의 위도
        double lon1 = glist.get(0).nowLongitude;//이이전의 경도
        double lon2 = glist.get(1).nowLongitude;//이전의 경도

        double x1 = Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lon1));
        double y1 = Math.sin(Math.toRadians(lat1));
        double z1 = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lon1));

        double x2 = Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(lon2));
        double y2 = Math.sin(Math.toRadians(lat2));
        double z2 = Math.cos(Math.toRadians(lat2)) * Math.sin(Math.toRadians(lon2));

        double[] v = new double[3];
        v[0] = x2 - x1;
        v[1] = y2 - y1;
        v[2] = z2 - z1;

        return v;
    }

    public Gps gpsByv() //벡터를 이용해 위치 예측
    {
        System.out.println("GPS BY VECTOR()");
        if (GPSQUEUE.size() < 2)
        {
            return this; //현재 이용자의 2번째 데이터 까지는 현재 위치 그대로 반환.
        }
        Gps resgps;
        double[] v = get_vector(); //이전 위치들의 방향벡터 구하기
        double[] resv;
        double k = available_Distance(); //평균 이동거리 
        double x = k * v[0];
        double y = k * v[1];
        double z = k * v[2];  //방향벡터에 길이 곱해서 같은 방향으로의 길이 N짜리 벡터 구하기
        double lat;
        double lon;

        resv = get_past_coordinate(); //이전 좌표
        resv[0] += x;
        resv[1] += y;
        resv[2] += z;  //이전 벡터에 이동 예측 벡터 더하기

        lat = Math.toDegrees(Math.asin(y));
        lon = Math.toDegrees(Math.atan(z/x)); //예측한 벡터의 위도 경도 구하기

        resgps = new Gps(-1, lat, lon, null, -1);

        return resgps;
	}

    public Queue<Gps> aroundGpsList(List<Gps> AroundGps, int num)
    {
        int count = 0;
        double min_d = 9999;
        double av_d = 0;
        double av_lat;
        double av_lon;
        double maxY;
        double minY;
        double maxX;
        double minX;

        
        if (GPSQUEUE.size() > 0) //이전 데이터가 있는 경우
        {
            av_d = available_Distance();
            av_lat = av_d / mForLatitude;
            av_lon = av_d / mForLongitude;
    
            //이전 좌표 기준 검색 거리 좌표
            maxY = glist.get(1).nowLatitude + av_lat;
            minY = glist.get(1).nowLatitude - av_lat;
            maxX = glist.get(1).nowLongitude + av_lon;
            minX = glist.get(1).nowLongitude - av_lon;
        }

        else //이전 데이터가 없는 경우
        {
            av_d = available_Distance(10);

            av_lat = av_d / mForLatitude;
            av_lon = av_d / mForLongitude;
    
            //현재 좌표 기준 검색 거리 좌표
            maxY = nowLatitude + av_lat;
            minY = nowLatitude - av_lat;
            maxX = nowLongitude + av_lon;
            minX = nowLongitude - av_lon;
        }

        //근처의 데이터 추출
        List<Gps>tempList = new ArrayList<>();
        for (Gps aroundGps : AroundGps)
        {
            if ((aroundGps.nowLatitude <= maxY && aroundGps.nowLatitude >= minY) && (aroundGps.nowLongitude <= maxX && aroundGps.nowLongitude >= minX))
            {
                tempList.add(aroundGps);
            }
        }

        if (tempList.isEmpty())
        {
            System.out.println("!!There is no data in this area!!");
            return null;
        }

        if (num > tempList.size())
        {
            num = tempList.size();
            System.out.println("NUM ALTERED: " + num);
        }

        Queue<Gps> resultQueue = new LinkedList<>();

        double[][] temp = new double[num][2];
        double distance;
        //현재 위치와 가장 가까운 num개 추출 
        for (Gps aGps : tempList)
        {
            distance = get_distance(this, aGps);

            if (count < num) //초기 데이터 입력
            {
                temp[count][0] = count;
                temp[count][1] = distance;
                count++;
            }

            else if (distance <= min_d) //초기데이터 큐에 입력 후, 새로운 데이터의 거리가 더 짧은 경우 poll 및 add
            {
                min_d = distance;
                resultQueue.poll();
                resultQueue.add(aGps);
            }

            if (count == num) //초기데이터 정렬 및 큐에 입력
            {
                if (num > 1)
                {
                    Arrays.sort(temp, (o1, o2) -> {
                        return (int)(o2[1] - o1[1]);
                    });
                }

                for (int i = 0; i < num; i++)
                {
                    resultQueue.add(tempList.get((int)temp[i][0]));
                }
                min_d = temp[num - 1][1];
                count++;
            }
        }
        return resultQueue;
    }

    public Gps avgLocation(Queue<Gps> gQueue)
    {
        Gps gps;
        Gps resgps;
        double sumlat = nowLatitude;
        double sumlon = nowLongitude;
        double avglat = 0;
        double avglon = 0;
        int num = gQueue.size();
        while (!gQueue.isEmpty())
        {
            gps = gQueue.poll();
            sumlat += gps.nowLatitude;
            sumlon += gps.nowLongitude;
        }
        avglat = sumlat / (num + 1);
        avglon = sumlon / (num + 1);

        resgps = new Gps(-1, avglat, avglon, time, -1);

        if(GPSQUEUE.size() >= 1)
            System.out.println("\n\nDISTANCE BETWEEN PREVIOUS DATA: " + get_distance(GPSQUEUE.peekLast(), resgps));;
        return resgps;
    }
}
