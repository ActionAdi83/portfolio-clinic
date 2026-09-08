// __API_URL__ / __KEYCLOAK_URL__ are substituted at Docker build time (see
// frontend/Dockerfile) so one image works for any deployment host — Angular
// resolves environment.ts at build time, so there is nothing to change on the
// server afterwards.
export const environment = {
  production: true,
  baseurl: '__API_URL__/api/',
  keycloak: {
    url: '__KEYCLOAK_URL__',
    realm: 'clinic',
    clientId: 'frontend'
  }
};
