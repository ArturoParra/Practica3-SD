package org.example;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DndApiService {
    // Cliente HTTP para hacer las peticiones
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Gson gson = new Gson();

    // Clase interna para mapear la lista de monstruos
    private static class ApiResourceList {
        ApiResource[] results;
    }

    public static class ApiResource {
        String index;
        String name;
    }

    // Método para obtener la lista completa de monstruos
    public ApiResource[] fetchMonsterList() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.dnd5eapi.co/api/2014/monsters"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        ApiResourceList resourceList = gson.fromJson(response.body(), ApiResourceList.class);
        return resourceList.results;
    }

    // Método para obtener los detalles de UN monstruo
    private Monster fetchMonsterfromAPI(String monsterIndex) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.dnd5eapi.co/api/monsters/" + monsterIndex))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("DEBUG: API Response for '" + monsterIndex + "': " + response.body());

        MonsterDetails details = gson.fromJson(response.body(), MonsterDetails.class);

        // Construir la URL completa de la imagen
        String imageUrl = "https://www.dnd5eapi.co" + details.image;

        // Filtrar solo las acciones que tienen información de daño
        List<Action> validActions = Arrays.stream(details.actions)
                .filter(action -> action.damage != null && action.damage.length > 0)
                .collect(Collectors.toList());

        // Crear y devolver nuestro objeto Monster con toda la información
        return new Monster(details.name, details.hit_points, details.hit_points, imageUrl, validActions);
    }

    public Monster getMonsterDetails(String monsterIndex, Map<String, Monster> cache) throws Exception {
        // 1. Check if the monster is already in the cache
        if (cache.containsKey(monsterIndex)) {
            System.out.println("CACHE HIT: Found '" + monsterIndex + "' in cache.");
            return cache.get(monsterIndex);
        }

        // 2. If not, fetch from the API
        System.out.println("CACHE MISS: Fetching '" + monsterIndex + "' from API.");
        Monster monster = fetchMonsterfromAPI(monsterIndex);

        // 3. Store the result in the cache for next time
        cache.put(monsterIndex, monster);

        return monster;
    }

    // Clase interna para mapear solo los detalles que necesitamos del JSON del monstruo
    // Dentro de DndApiService.java
    private static class MonsterDetails {
        String name;
        int hit_points;
        String image; // <-- Nuevo: la ruta de la imagen
        Action[] actions; // <-- Nuevo: el array de acciones
    }
}
