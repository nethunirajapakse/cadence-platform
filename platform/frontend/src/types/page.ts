// Matches Spring Data's Page<T> JSON shape exactly - what /api/reports/mine
// and /api/reports (the dashboard) both return.
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page, 0-indexed
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
