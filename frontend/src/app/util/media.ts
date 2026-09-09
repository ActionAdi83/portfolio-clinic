import { environment } from '../../environment/environment';

/**
 * Resolves a stored image path ("services/images/<id>") against the API base
 * URL. Left untouched if it's already absolute, so a pasted external link
 * (from before uploads existed) still renders.
 */
export function imageUrl(path: string | null | undefined): string {
  if (!path) return '';
  return path.startsWith('http') ? path : `${environment.baseurl}${path}`;
}
