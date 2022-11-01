package tank.ooad.fitgub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

@Component
public class OauthService {
    private final static String client_secret = "a816d6e5e0cee016dfa8b0dcc4c6c0e54646ce5f";
    private final static String client_id = "8f71fe35e1823f5f5b87";

    public String accessToken(String code) {
        String url = "https://github.com/login/oauth/access_token?client_id=" + client_id + "&client_secret=" + client_secret + "&code=" + code;
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        System.out.println(response);
        assert response != null;
        return response.split("&")[0].split("=")[1];
    }

    public String userInfo(String token) {
        String url = "https://api.github.com/user";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Accept", "application/vnd.github+json");
        String response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        return response;
    }

    public JsonNode userEmail(String token) {
        String url = "https://api.github.com/user/emails";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Accept", "application/vnd.github+json");
        String response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(response);
            return node;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
