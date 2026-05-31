package com.sara.superadmin.security;

import com.sara.superadmin.model.Store;
import com.sara.superadmin.repository.StoreRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates store backends calling /api/store-api/* via the X-Store-Api-Key header.
 * On success, the authenticated principal is the Store's id so controllers know which
 * store is acting. We resolve by per-store apiKey; a global key is also accepted for
 * bootstrapping (the single B-Nutri store before its own key is wired up).
 */
@Component
public class StoreApiKeyFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Store-Api-Key";
	public static final String STORE_ID_ATTR = "storeId";

	private final StoreRepository storeRepository;

	public StoreApiKeyFilter(StoreRepository storeRepository) {
		this.storeRepository = storeRepository;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/store-api/");
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request,
									@NonNull HttpServletResponse response,
									@NonNull FilterChain filterChain)
			throws ServletException, IOException {

		String key = request.getHeader(HEADER);
		if (key != null && !key.isBlank()) {
			// Strict: resolve the store solely by its own per-store apiKey. No global fallback.
			Optional<Store> store = storeRepository.findByApiKey(key);

			if (store.isPresent() && store.get().isEnabled()) {
				Store s = store.get();
				request.setAttribute(STORE_ID_ATTR, s.getId());
				var auth = new UsernamePasswordAuthenticationToken(
						s.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_STORE")));
				SecurityContextHolder.getContext().setAuthentication(auth);
			}
		}
		filterChain.doFilter(request, response);
	}
}
