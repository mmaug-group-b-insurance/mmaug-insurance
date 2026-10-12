import { env } from "./env";

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  correlationId?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
}

export class ApiRequestError extends Error {
  status: number;
  code: string;
  correlationId?: string;
  fieldErrors?: ApiError["fieldErrors"];
  constructor(err: ApiError) {
    super(err.message);
    this.name = "ApiRequestError";
    this.status = err.status;
    this.code = err.code;
    this.correlationId = err.correlationId;
    this.fieldErrors = err.fieldErrors;
  }
}

type TokenProvider = () => Promise<string | null>;

let tokenProvider: TokenProvider = async () => null;
export function setTokenProvider(fn: TokenProvider) { tokenProvider = fn; }

export interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "DELETE" | "PUT";
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined | null | string[]>;
  signal?: AbortSignal;
  raw?: boolean;
  formData?: FormData;
}

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const base = env.apiBaseUrl.replace(/\/+$/, "");
  const url = new URL(`${base}${path.startsWith("/") ? path : `/${path}`}`);
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v === undefined || v === null) continue;
      if (Array.isArray(v)) v.forEach((item) => url.searchParams.append(k, String(item)));
      else url.searchParams.set(k, String(v));
    }
  }
  return url.toString();
}

export async function apiRequest<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const headers: HeadersInit = { Accept: "application/json" };
  const token = await tokenProvider();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let body: BodyInit | undefined;
  if (opts.formData) {
    body = opts.formData;
  } else if (opts.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(opts.body);
  }

  let res: Response;
  try {
    res = await fetch(buildUrl(path, opts.query), {
      method: opts.method ?? (body ? "POST" : "GET"),
      headers,
      body,
      signal: opts.signal,
      credentials: "include",
    });
  } catch (err) {
    if ((err as Error).name === "AbortError") throw err;
    throw new ApiRequestError({
      timestamp: new Date().toISOString(),
      status: 0,
      code: "NETWORK_ERROR",
      message: "Unable to reach the server. Please check your connection and try again.",
      path,
    });
  }

  if (res.status === 204) return undefined as T;

  const contentType = res.headers.get("content-type") ?? "";
  const isJson = contentType.includes("application/json");

  if (!res.ok) {
    let payload: ApiError | undefined;
    if (isJson) { try { payload = (await res.json()) as ApiError; } catch { /* ignore */ } }
    throw new ApiRequestError(
      payload ?? {
        timestamp: new Date().toISOString(),
        status: res.status,
        code: `HTTP_${res.status}`,
        message: res.statusText || "Request failed",
        path,
      }
    );
  }

  if (opts.raw) return res as unknown as T;
  if (isJson) return (await res.json()) as T;
  return (await res.text()) as unknown as T;
}