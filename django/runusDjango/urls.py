"""
URL configuration for runusDjango project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/4.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.conf import settings
from django.conf.urls.static import static
from rest_framework import routers
from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
)
from rest_framework.authtoken.views import obtain_auth_token
from django.contrib import admin
from django.urls import path, include


from DATA_APP import views

router = routers.DefaultRouter()
router.register(r"tests", views.TestViewSet)

urlpatterns = [
    path("admin/", admin.site.urls),
    path("DATA_APP/", include("DATA_APP.urls")),
    path("account/", include("account.urls")),
    path("history/", include("history.urls")),
    path("api/token/", TokenObtainPairView.as_view(), name="token_obtain_pair"),
    path("api/token/refresh/", TokenRefreshView.as_view(), name="token_refresh"),
    path("", include(router.urls)),
    path("api-token-auth/", obtain_auth_token, name="api_token_auth"),
] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
