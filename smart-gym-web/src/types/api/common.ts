export interface ApiEnvelope<T> {
    success: boolean
    code: string
    message: string
    data: T
    traceId: string
    timestamp: string
  }
  
  export interface PageData<T> {
    items: T[]
    page: number
    pageSize: number
    total: number
    totalPages: number
  }
  
  export interface PageQuery { page?: number; pageSize?: number }