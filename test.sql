SELECT
    a.user_id,
    JSON_AGG(
        DISTINCT jsonb_build_object(
            'street', a.street,
            'city', a.city,
            'tags', tags.tags
        )
    ) AS addresses
FROM user_address a
LEFT JOIN (
    SELECT
        at.address_id,
        JSON_AGG(DISTINCT t.name) AS tags
    FROM address_tag at
    JOIN tag t ON t.id = at.tag_id
    GROUP BY at.address_id
) tags
    ON tags.address_id = a.id
GROUP BY a.user_id
