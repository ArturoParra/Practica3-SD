import React from 'react'

const MonsterCard = ({ monster }) => {

    const healthPercentage = (monster.hp / monster.maxHp) * 100;

    return (
        <div className="flex flex-col items-center justify-centerw-1/3 text-center">
            <h2 className="text-3xl font-bold mb-5">{monster.name}</h2>
            <img src={monster.imageUrl} alt={monster.name} className="h-64 object-contain mb-2 rounded-md shadow-2xl" />
            <div className="bg-gray-500 h-6 w-full mt-2 rounded-full">
                <div className={`h-full rounded-full ${healthPercentage > 60 ? 'bg-green-500' : 'bg-amber-500'} ${healthPercentage < 30 ? 'bg-red-500' : ''}`} style={{ width: `${healthPercentage}%` }}></div>
            </div>
            <p className="mt-2 text-lg">{monster.hp} / {monster.maxHp} HP</p>

        </div>
    )
}

export default MonsterCard