# Generated by Django 4.2.5 on 2023-11-02 02:13

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('history', '0003_alter_history_sectional_record'),
    ]

    operations = [
        migrations.RenameField(
            model_name='history',
            old_name='sectional_record',
            new_name='sectional_speed',
        ),
    ]
