import requests
r = requests.get('http://192.168.81.179:3000/gps/').text
print(r)