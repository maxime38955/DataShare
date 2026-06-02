import http from 'k6/http';
import { check, sleep } from 'k6';
 
export const options = {
  stages: [
    { duration: '10s', target: 20 }, 
    { duration: '30s', target: 20 }, 
    { duration: '10s', target: 0 },  
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], 
    http_req_failed: ['rate<0.01'],   
  },
};

 
const BASE_URL = 'http://localhost:8080/api/v1'; 

export default function () {
  
 
  const loginPayload = JSON.stringify({
    email: 'test@test.com',  
    password: 'test@test.com',
  });

  const loginParams = {
    headers: { 'Content-Type': 'application/json' },
  };

  // Appel de la vraie route : http://localhost:8080/api/v1/user/login
  const loginRes = http.post(`${BASE_URL}/user/login`, loginPayload, loginParams);

  check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'has jwt token': (r) => r.json('token') !== undefined,
  });

  
  if (loginRes.status === 200) {
    const token = loginRes.json('token');

    const authParams = {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    };

    // Appel de la vraie route : http://localhost:8080/api/v1/files/user/files
    const filesRes = http.get(`${BASE_URL}/files/user/files`, authParams);

    check(filesRes, {
      'files status is 200': (r) => r.status === 200,
    });
  } else {
    console.log(`Échec du login. Code HTTP: ${loginRes.status} - Body: ${loginRes.body}`);
  }

  sleep(1);
}