package com.cyebrcina.pos.data.remote

import com.cyebrcina.pos.data.remote.model.AcceptOrderResponse
import com.cyebrcina.pos.data.remote.model.CallsResponse
import com.cyebrcina.pos.data.remote.model.CashRegisterSessionResponse
import com.cyebrcina.pos.data.remote.model.ChargeOrderRequest
import com.cyebrcina.pos.data.remote.model.ChargeOrderResponse
import com.cyebrcina.pos.data.remote.model.ClockRequest
import com.cyebrcina.pos.data.remote.model.ClockResponse
import com.cyebrcina.pos.data.remote.model.CloseRegisterRequest
import com.cyebrcina.pos.data.remote.model.CreateOrderRequest
import com.cyebrcina.pos.data.remote.model.CreateOrderResponse
import com.cyebrcina.pos.data.remote.model.LoginRequest
import com.cyebrcina.pos.data.remote.model.LoginResponse
import com.cyebrcina.pos.data.remote.model.OpenRegisterRequest
import com.cyebrcina.pos.data.remote.model.MenuResponse
import com.cyebrcina.pos.data.remote.model.OrderHistoryResponse
import com.cyebrcina.pos.data.remote.model.PaymentLinkResponse
import com.cyebrcina.pos.data.remote.model.PendingOrdersResponse
import com.cyebrcina.pos.data.remote.model.ReceiptData
import com.cyebrcina.pos.data.remote.model.RejectOrderResponse
import com.cyebrcina.pos.data.remote.model.RepeatOrderResponse
import com.cyebrcina.pos.data.remote.model.SavePhoneContactRequest
import com.cyebrcina.pos.data.remote.model.SavePhoneContactResponse
import com.cyebrcina.pos.data.remote.model.KitchenTicketData
import com.cyebrcina.pos.data.remote.model.SetStoreStatusRequest
import com.cyebrcina.pos.data.remote.model.SetStoreStatusResponse
import com.cyebrcina.pos.data.remote.model.StoreStatusResponse
import com.cyebrcina.pos.data.remote.model.TablesResponse
import com.cyebrcina.pos.data.remote.model.ToggleSoldOutResponse
import com.cyebrcina.pos.data.remote.model.ZReport
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Fire Hut's real device/EPOS API — one method per path in
 * `openapi.yaml` at the repo root. Base URL is `BuildConfig.DEVICE_API_BASE_URL`
 * (https://admin.firehut.uk). Auth (`Authorization: Bearer <token>`) is attached by the
 * OkHttp interceptor in [com.cyebrcina.pos.di.NetworkModule], not per-call here.
 */
interface FireHutDeviceApi {

    @POST("api/device/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/device/menu")
    suspend fun menu(): Response<MenuResponse>

    @GET("api/device/orders/pending")
    suspend fun pendingOrders(): Response<PendingOrdersResponse>

    @GET("api/device/orders/history")
    suspend fun orderHistory(@Query("limit") limit: Int = 50): Response<OrderHistoryResponse>

    @POST("api/device/orders/{id}/accept")
    suspend fun acceptOrder(@Path("id") orderId: String): Response<AcceptOrderResponse>

    @POST("api/device/orders/{id}/reject")
    suspend fun rejectOrder(@Path("id") orderId: String): Response<RejectOrderResponse>

    @GET("api/device/orders/{id}/receipt")
    suspend fun receiptData(@Path("id") orderId: String): Response<ReceiptData>

    @GET("api/device/orders/{id}/ticket")
    suspend fun ticketData(@Path("id") orderId: String): Response<KitchenTicketData>

    @GET("api/device/store-status")
    suspend fun getStoreStatus(): Response<StoreStatusResponse>

    @POST("api/device/store-status")
    suspend fun setStoreStatus(@Body request: SetStoreStatusRequest): Response<SetStoreStatusResponse>

    @GET("api/device/z-report")
    suspend fun zReport(@Query("date") date: String? = null, @Query("channel") channel: String? = null): Response<ZReport>

    /** Real, live. Every active table's real occupancy/status, synced from Admin. */
    @GET("api/device/tables")
    suspend fun tables(): Response<TablesResponse>

    /**
     * NOT YET LIVE — see BACKEND_CARD_PAYMENT_SPEC.md. Records an at-till card charge (e.g. for
     * a DINE_IN order) against an order. Will 404 until the backend implements this route.
     */
    @POST("api/device/orders/{id}/charge")
    suspend fun chargeOrder(@Path("id") orderId: String, @Body request: ChargeOrderRequest): Response<ChargeOrderResponse>

    /** Real, live. Submits a till-built order (menu items + payment already taken at the till). */
    @POST("api/device/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<CreateOrderResponse>

    /** Real, live. Creates a Stripe Checkout scan-to-pay QR for an order's outstanding balance. */
    @POST("api/device/orders/{id}/payment-link")
    suspend fun paymentLink(@Path("id") orderId: String): Response<PaymentLinkResponse>

    /** Real, live. Saves/updates a caller's name/address/notes from the Incoming Call popup. */
    @POST("api/device/phone-contacts")
    suspend fun savePhoneContact(@Body request: SavePhoneContactRequest): Response<SavePhoneContactResponse>

    /** Real, live. Recent incoming calls for the till's own Calls tab. */
    @GET("api/device/calls")
    suspend fun calls(): Response<CallsResponse>

    /** Real, live. Clocks a staff member in/out by PIN — same StaffClockEvent model Admin's kiosk uses. */
    @POST("api/device/staff/clock")
    suspend fun clockStaff(@Body request: ClockRequest): Response<ClockResponse>

    /** Real, live. This till's currently open cash register session, if any. */
    @GET("api/device/register/current")
    suspend fun currentRegisterSession(): Response<CashRegisterSessionResponse>

    /** Real, live. Opens this till's register for the shift with a counted starting float. */
    @POST("api/device/register/open")
    suspend fun openRegister(@Body request: OpenRegisterRequest): Response<CashRegisterSessionResponse>

    /** Real, live. Closes this till's register with a physical cash count; reconciles against
     * this till's own CASH sales during the session. */
    @POST("api/device/register/close")
    suspend fun closeRegister(@Body request: CloseRegisterRequest): Response<CashRegisterSessionResponse>

    /** Real, live. 86s a product or brings it back — the till's own Tools/86 Board. */
    @POST("api/device/menu/products/{id}/toggle-sold-out")
    suspend fun toggleProductSoldOut(@Path("id") productId: String): Response<ToggleSoldOutResponse>

    /** Real, live. A past order's line items (with productId/sizeId/addOnIds), for the Incoming
     * Call popup's "Repeat Last Order" action. */
    @GET("api/device/orders/repeat/{id}")
    suspend fun repeatOrder(@Path("id") orderId: String): Response<RepeatOrderResponse>
}
