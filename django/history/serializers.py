from rest_framework import serializers
from .models import history, group_history


class HistorySerializer(serializers.ModelSerializer):
    class Meta:
        model = history
        fields = "__all__"


class GroupHistorySerializer(serializers.ModelSerializer):
    class Meta:
        model = group_history
        fields = "__all__"


class RecentHistorySerializer(serializers.ModelSerializer):
    class Meta:
        model = history
        fields = ("distance", "duration")


class MonthlyDataSerializer(serializers.Serializer):
    distance = serializers.FloatField()
    time = serializers.DurationField()
    calories = serializers.FloatField()


class DailyDataSerializer(serializers.Serializer):
    distance = serializers.FloatField()
    time = serializers.DurationField()
    calories = serializers.FloatField()