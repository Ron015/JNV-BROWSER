package ron.jnv.browser;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("Ron015/JNV-BROWSER/main/weballow.json")
    Call<List<Website>> getAllowedWebsites();
}